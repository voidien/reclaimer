package com.voidreclaimer.link;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.mixin.MinecraftServerStorageAccessor;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Moves a mind between bodies. This is a swap, not a remote camera: flight, tools and the HUD all need the player to be the body,
 * so the player goes to the shell and takes it on.
 *
 * <p>Each move is one transaction, and at every step each body is somewhere that survives. Everything that can refuse is asked
 * before anything changes. The main body is on record in {@link LinkState} before the player's own copy is overwritten, and a
 * shell is on record in the outbox before it is put back. If wearing a body throws, which an addon's listener can make it do, the
 * move is undone. It ends by saving the player file, the one store of record while linked.
 *
 * <p>Asked for from a command or a packet, a move runs between ticks ({@link #onServerTick}), never inside an entity's own tick.
 * A request holds ids, not objects: by the time it runs the player may have logged out or died.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Links {
    /** How a move ended. Only {@link #LINKED} and {@link #WOKEN} changed anything. */
    public enum Result {
        LINKED, WOKEN,
        /** Already in a shell: wake first. */
        ALREADY_LINKED,
        NOT_LINKED,
        /** Gone, dying, or not a shell at all. */
        NOT_A_SHELL,
        NOT_YOURS,
        /** The player may not go there, for instance a locked dimension. */
        CANNOT_REACH,
        /** The main body's tag cannot be read: the player stays in the shell and the tag stays in the player file. */
        HOME_UNREADABLE,
        /** The player logged out or died before the move ran. */
        PLAYER_GONE,
        /** Something threw part way, and the move was undone. */
        FAILED
    }

    /** A waiting move. No shell means a wake. */
    private record Move(UUID player, @Nullable ResourceKey<Level> shellLevel, @Nullable UUID shell, Consumer<Result> then) {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Queue<Move> MOVES = new ArrayDeque<>();

    private Links() {}

    public static LinkState state(ServerPlayer player) {
        return player.getData(ModAttachments.LINK);
    }

    /** Asks for {@link #link} at the end of this tick; {@code then} hears how it went. */
    public static void requestLink(ServerPlayer player, BodyHost shell, Consumer<Result> then) {
        MOVES.add(new Move(player.getUUID(), shell.level().dimension(), shell.getUUID(), then));
    }

    /** Asks for {@link #wake} at the end of this tick; {@code then} hears how it went. */
    public static void requestWake(ServerPlayer player, Consumer<Result> then) {
        MOVES.add(new Move(player.getUUID(), null, null, then));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (Move move = MOVES.poll(); move != null; move = MOVES.poll()) {
            move.then().accept(run(event.getServer(), move));
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        MOVES.clear();
    }

    private static Result run(MinecraftServer server, Move move) {
        ServerPlayer player = server.getPlayerList().getPlayer(move.player());
        if (player == null) {
            return Result.PLAYER_GONE;
        }
        if (move.shell() == null) {
            return wake(player);
        }
        ServerLevel level = server.getLevel(move.shellLevel());
        return level != null && level.getEntity(move.shell()) instanceof BodyHost shell ? link(player, shell) : Result.NOT_A_SHELL;
    }

    /** Puts {@code player} in {@code shell}. The main body goes into the player file and the shell leaves the world. */
    public static Result link(ServerPlayer player, BodyHost shell) {
        LinkState state = state(player);
        if (!present(player)) {
            return Result.PLAYER_GONE;
        }
        if (state.linked()) {
            return Result.ALREADY_LINKED;
        }
        if (shell.isRemoved() || shell.isDeadOrDying() || shell.role() != HostRole.SHELL || stale(shell)) {
            return Result.NOT_A_SHELL;
        }
        if (!player.getUUID().equals(shell.owner())) {
            return Result.NOT_YOURS;
        }
        ServerLevel there = (ServerLevel) shell.level();
        if (!mayEnter(player, there)) {
            return Result.CANNOT_REACH;
        }
        LinkState.Place from = place(player);
        ServerLevel here = player.serverLevel();
        float homeDust = Bodies.stats(player).dustCapacity();
        BodySnapshot home = BodySnapshot.take(player);
        CompoundTag homeTag = saved(home, player);
        if (homeTag == null) {
            return Result.FAILED;
        }
        BodySnapshot body = BodySnapshot.of(shell);
        if (!arrive(player, there, shell.position(), shell.getYRot(), shell.getXRot())) {
            return Result.CANNOT_REACH;
        }
        player.setData(ModAttachments.LINK, state.withoutOutbox(shell.getUUID()).withLink(new LinkState.Link(shell.getUUID(), shell.epoch() + 1, homeTag, from)));
        if (!wear(player, body)) {
            boolean restored = wear(player, home);
            arrive(player, here, from.position(), from.yaw(), from.pitch());
            if (restored) {
                player.setData(ModAttachments.LINK, state);
            } else {
                // The link stays, so the main body is not lost, and so does the shell: it must not count as a copy left from before it.
                shell.setEpoch(shell.epoch() + 1);
            }
            return Result.FAILED;
        }
        setHomeDust(player, homeDust);
        shell.discard();
        settle(player);
        saveNow(player.server);
        return Result.LINKED;
    }

    /** Brings {@code player} back to the main body, and puts the shell back in the world where they were. */
    public static Result wake(ServerPlayer player) {
        LinkState state = state(player);
        if (!present(player)) {
            return Result.PLAYER_GONE;
        }
        Optional<LinkState.Link> found = state.link();
        if (found.isEmpty()) {
            return Result.NOT_LINKED;
        }
        LinkState.Link link = found.get();
        Optional<BodySnapshot> home = BodySnapshot.load(link.home(), player.registryAccess());
        if (home.isEmpty()) {
            return Result.HOME_UNREADABLE;
        }
        ServerLevel target = player.server.getLevel(link.from().dimension());
        if (target == null || !mayEnter(player, target)) {
            return Result.CANNOT_REACH;
        }
        LinkState.Place at = place(player);
        ServerLevel shellLevel = player.serverLevel();
        BodySnapshot body = BodySnapshot.take(player);
        CompoundTag bodyTag = saved(body, player);
        if (bodyTag == null) {
            return Result.FAILED;
        }
        if (!arrive(player, target, link.from().position(), link.from().yaw(), link.from().pitch())) {
            return Result.CANNOT_REACH;
        }
        LinkState.Outbox outbox = new LinkState.Outbox(link.shell(), link.epoch(), bodyTag, at, false);
        player.setData(ModAttachments.LINK, state.withOutbox(outbox));
        Optional<BodyHost> shell = BodyHost.restore(shellLevel, at, link.shell(), link.epoch(), body, player.getUUID());
        if (shell.isEmpty()) {
            LOGGER.warn("Shell {} could not be put back in the world yet: it stays in the outbox", link.shell());
        }
        if (!wear(player, home.get())) {
            shell.ifPresent(BodyHost::discard);
            wear(player, body);
            arrive(player, shellLevel, at.position(), at.yaw(), at.pitch());
            player.setData(ModAttachments.LINK, state);
            return Result.FAILED;
        }
        shell.ifPresent(host -> Outboxes.settled(host.getUUID()));
        player.setData(ModAttachments.LINK, state.withOutbox(outbox).withoutLink());
        setHomeDust(player, DustData.NOT_LINKED);
        settle(player);
        saveNow(player.server);
        return Result.WOKEN;
    }

    private static void setHomeDust(ServerPlayer player, float capacity) {
        player.setData(ModAttachments.HOME_DUST_CAPACITY, capacity);
        player.syncData(ModAttachments.HOME_DUST_CAPACITY);
    }

    /** What ends a move: the player's state on disk, in both places a server reads it from. */
    private static void saveNow(MinecraftServer server) {
        server.getPlayerList().saveAll();
        // Singleplayer loads its owner from level.dat, which only an autosave or a quit writes, not the player file.
        CompoundTag host = server.getPlayerList().getSingleplayerData();
        if (host != null) {
            ((MinecraftServerStorageAccessor) server).void_reclaimer$storageSource().saveDataTag(server.registryAccess(), server.getWorldData(), host);
        }
    }

    /** A shell destroyed for good: nothing is to bring it back. A shell in a wreck comes with the destroyed path. */
    static void destroyed(BodyHost shell) {
        ServerPlayer owner = owner(shell);
        if (owner != null) {
            owner.setData(ModAttachments.LINK, state(owner).withoutOutbox(shell.getUUID()));
        }
    }

    @Nullable
    private static ServerPlayer owner(BodyHost shell) {
        return shell.owner() == null ? null : shell.level().getServer().getPlayerList().getPlayer(shell.owner());
    }

    /**
     * Whether this copy of a shell lost to a newer one: its owner is in it, or a newer copy that can be read waits in the outbox. Only
     * a crash leaves such a copy, on disk from before the link. With its owner away nobody can tell, and nobody can use it either.
     */
    public static boolean stale(BodyHost shell) {
        ServerPlayer owner = owner(shell);
        if (owner == null) {
            return false;
        }
        LinkState state = state(owner);
        return state.link().filter(link -> link.shell().equals(shell.getUUID()) && link.epoch() > shell.epoch()).isPresent()
                || state.outbox(shell.getUUID()).filter(outbox -> outbox.epoch() > shell.epoch()
                        && BodySnapshot.load(outbox.body(), owner.registryAccess()).isPresent()).isPresent();
    }

    /** Whether there is still a living player here to move: a stale object or a corpse would take a body with it. */
    private static boolean present(ServerPlayer player) {
        return !player.isRemoved() && !player.hasDisconnected() && player.isAlive();
    }

    /** Asked before anything changes. A move inside one dimension needs no one's leave. */
    private static boolean mayEnter(ServerPlayer player, ServerLevel target) {
        return target == player.serverLevel() || CommonHooks.onTravelToDimension(player, target.dimension());
    }

    @Nullable
    private static CompoundTag saved(BodySnapshot body, ServerPlayer player) {
        try {
            return body.save(player.registryAccess());
        } catch (RuntimeException e) {
            LOGGER.error("A body could not be saved, so {} stays where they are", player.getScoreboardName(), e);
            return null;
        }
    }

    /** Takes the player there; false if they did not get there. A move out of a Helm seat is part of this. */
    private static boolean arrive(ServerPlayer player, ServerLevel level, Vec3 position, float yaw, float pitch) {
        player.teleportTo(level, position.x, position.y, position.z, Set.of(), yaw, pitch);
        return player.serverLevel() == level;
    }

    /** Puts a body on the player. An addon's listener can throw from in there, and a move has to survive that. */
    private static boolean wear(ServerPlayer player, BodySnapshot body) {
        try {
            body.applyTo(player);
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("A body could not be put on {}", player.getScoreboardName(), e);
            return false;
        }
    }

    private static LinkState.Place place(ServerPlayer player) {
        return new LinkState.Place(player.level().dimension(), player.position(), player.getYRot(), player.getXRot());
    }

    /**
     * What belongs to neither body starts fresh: the speed check, a held recall, a fall. A body arrives at rest: flight runs on the
     * client, so the server has no speed to hand over. Held keys stay, because the client only reports a change.
     */
    private static void settle(ServerPlayer player) {
        player.getData(ModAttachments.FLIGHT_TRACKER).clear();
        player.removeData(ModAttachments.RECALL_STATE);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
}
