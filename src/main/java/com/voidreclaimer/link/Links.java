package com.voidreclaimer.link;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.MainBodyWreckedEvent;
import com.voidreclaimer.api.event.ShellLinkEvent;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.core.BackupCores;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.mixin.MinecraftServerStorageAccessor;
import com.voidreclaimer.core.DeathMarker;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import com.voidreclaimer.network.DeathShellsPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.tether.TetherData;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.SpawnHandler;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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
        /** Its chunks were read, and it is not where the list says. */
        NOT_FOUND,
        /** Its chunks took too long to load. */
        TIMED_OUT,
        /** A link is on its way already. */
        WAITING,
        /** The seat's signal does not reach the shell. */
        OUT_OF_RANGE,
        /** An addon said no ({@link ShellLinkEvent.Pre}). */
        CANCELLED,
        /** Something threw part way, and the move was undone. */
        FAILED
    }

    /** A waiting move. No shell means a wake, and {@code end} says why. */
    private record Move(UUID player, @Nullable ResourceKey<Level> shellLevel, @Nullable UUID shell, @Nullable UUID seat, ShellLinkEvent.Reason end, Consumer<Result> then) {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Queue<Move> MOVES = new ArrayDeque<>();
    /** Who picked a shell on the death screen. Lives on the server thread. */
    private static final Map<UUID, UUID> WAKE_CHOICES = new HashMap<>();
    /** The link number of a wreck that has never been a shell. */
    private static final long FIRST_EPOCH = 1L;

    private Links() {}

    public static LinkState state(ServerPlayer player) {
        return player.getData(ModAttachments.LINK);
    }

    /** Asks for {@link #link} at the end of this tick; {@code then} hears how it went. */
    public static void requestLink(ServerPlayer player, BodyHost shell, Consumer<Result> then) {
        requestLink(player, shell, null, then);
    }

    /** {@link #requestLink(ServerPlayer, BodyHost, Consumer)} from the Uplink Seat with id {@code seat}, if any. */
    static void requestLink(ServerPlayer player, BodyHost shell, @Nullable UUID seat, Consumer<Result> then) {
        MOVES.add(new Move(player.getUUID(), shell.level().dimension(), shell.getUUID(), seat, ShellLinkEvent.Reason.LEFT, then));
    }

    /** Asks for {@link #link} to a shell anywhere, by the player's list of shells: see {@link ShellReach}. */
    public static void requestLink(ServerPlayer player, UUID shell, Consumer<Result> then) {
        ShellReach.request(player, shell, then);
    }

    /** {@link #requestLink(ServerPlayer, UUID, Consumer)} from the Uplink Seat with id {@code seat}. */
    static void requestLink(ServerPlayer player, UUID shell, UUID seat, Consumer<Result> then) {
        ShellReach.request(player, shell, seat, then);
    }

    /** Asks for {@link #wake} at the end of this tick; {@code then} hears how it went. */
    public static void requestWake(ServerPlayer player, Consumer<Result> then) {
        requestWake(player, ShellLinkEvent.Reason.LEFT, then);
    }

    /** {@link #requestWake(ServerPlayer, Consumer)} for {@code why}, which {@link ShellLinkEvent.Ended} reports. */
    static void requestWake(ServerPlayer player, ShellLinkEvent.Reason why, Consumer<Result> then) {
        MOVES.add(new Move(player.getUUID(), null, null, null, why, then));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (Move move = MOVES.poll(); move != null; move = MOVES.poll()) {
            move.then().accept(run(event.getServer(), move));
        }
    }

    /**
     * Two copies of a shell meet: a crash left one on disk from before a link, and both chunks have loaded. A level holds one entity
     * per id and refuses the second with only a warning, and the chunk of a refused copy then saves without it. So the older copy
     * has to give way here, whichever came first. An older arrival needs nothing: the level refuses it. This asks nothing of the
     * owner, who may be away. Last in line, because a listener after this one could cancel the arrival with the older copy already
     * gone. A copy in a chunk on its way out cannot be seen here; see {@link #replaced} for why that costs a rollback at worst.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof BodyHost arriving && event.getLevel() instanceof ServerLevel level
                && level.getEntity(arriving.getUUID()) instanceof BodyHost held && held != arriving && arriving.epoch() > held.epoch()) {
            LOGGER.info("Shell {}: the copy with link number {} gives way to the one with {}", held.getUUID(), held.epoch(), arriving.epoch());
            held.discard();
        }
    }

    /**
     * A body dying around the player is not the player dying: the mind goes home, and the shell becomes a wreck. Cancelled before
     * anything else hears of it, so no items fall, no death marker is set and no Backup Core charge is spent. The move waits for the
     * end of the tick, as every move does, and the health keeps the body standing until then.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && state(player).linked()) {
            event.setCanceled(true);
            player.setHealth(1f);
            MOVES.add(new Move(player.getUUID(), null, null, null, ShellLinkEvent.Reason.BODY_LOST, result -> {
                if (result == Result.WOKEN) {
                    player.displayClientMessage(Component.translatable("message.void_reclaimer.link.lost"), false);
                } else if (result != Result.NOT_LINKED) {
                    LOGGER.warn("{} lost a shell and could not wake: {}", player.getScoreboardName(), result);
                }
            }));
        }
    }

    /** Last in line, so a mod that cancels the death loses nothing. With the game rule that keeps inventories, the body is kept too. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onMainBodyDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            if (!player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                leaveWreck(player);
            }
            WAKE_CHOICES.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, new DeathShellsPayload(Signal.rows(player).stream().filter(row -> !ShellRow.WRECK.equals(row.status()) && !row.recoverable()).toList()));
        }
    }

    /** The death screen's choice: wake in this shell, or nowhere special. It only lasts until the respawn, so it isn't saved. */
    public static void chooseWake(ServerPlayer player, Optional<UUID> shell) {
        shell.filter(id -> state(player).known(id).isPresent()).ifPresentOrElse(id -> WAKE_CHOICES.put(player.getUUID(), id), () -> WAKE_CHOICES.remove(player.getUUID()));
    }

    /** A shell is a link like any other, so the player has a body to come back to: the new one, where it respawned. */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        UUID shell = WAKE_CHOICES.remove(event.getEntity().getUUID());
        if (shell != null && event.getEntity() instanceof ServerPlayer player) {
            requestLink(player, shell, result -> {
                if (result != Result.LINKED) {
                    LOGGER.warn("{} chose to wake in shell {} and could not: {}", player.getScoreboardName(), shell, result);
                    player.displayClientMessage(Component.translatable("message.void_reclaimer.respawn.shell_failed"), false);
                }
            });
        }
    }

    /**
     * The main body stays where it died as a wreck, with everything it carried and was built from, and what it paid into the Replicator
     * queue. The player goes on in a new starter body. The order is {@link #wake}'s, and for the same reason: the player file and the
     * chunk save apart, so the body is in the outbox before it leaves the player, and a crash at any point leaves it in one place.
     */
    private static void leaveWreck(ServerPlayer player) {
        LinkState state = state(player);
        BodySnapshot taken = BodySnapshot.take(player);
        ReplicatorLogic.Lost queued = Replicator.leftBy(player, Optional.empty());
        BodySnapshot body = taken.withPending(queued.items());
        CompoundTag tag = saved(body, player);
        if (tag == null) {
            return;
        }
        UUID id = UUID.randomUUID();
        LinkState.Place at = place(player);
        player.setData(ModAttachments.LINK, state.withOutbox(new LinkState.Outbox(id, FIRST_EPOCH, tag, at, false))
                .withKnown(new LinkState.KnownShell(id, FIRST_EPOCH, at, true, false)));
        Optional<BodyHost> wreck = restored(player.serverLevel(), at, id, FIRST_EPOCH, body, player);
        wreck.ifPresent(host -> {
            Outboxes.settled(id);
            Wrecks.make(host);
            Wrecks.setDrifting(host);
        });
        DeathMarker.point(player, id, GlobalPos.of(at.dimension(), BlockPos.containing(at.position())));
        Replicator.settle(player, queued);
        player.getInventory().clearContent();
        player.setData(ModAttachments.TETHER, TetherData.NONE);
        player.syncData(ModAttachments.TETHER);
        Bodies.store(player, Bodies.starter());
        EnergyData energy = player.getData(ModAttachments.ENERGY);
        Energy.update(player, new EnergyData(energy.capacity(), energy.capacity(), false), true);
        saveNow(player.server);
        NeoForge.EVENT_BUS.post(new MainBodyWreckedEvent(player, id));
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        MOVES.clear();
        WAKE_CHOICES.clear();
    }

    private static Result run(MinecraftServer server, Move move) {
        ServerPlayer player = server.getPlayerList().getPlayer(move.player());
        if (player == null) {
            return Result.PLAYER_GONE;
        }
        if (move.shell() == null) {
            return wake(player, move.end());
        }
        ServerLevel level = server.getLevel(move.shellLevel());
        return level != null && level.getEntity(move.shell()) instanceof BodyHost shell ? link(player, shell, move.seat()) : Result.NOT_A_SHELL;
    }

    /** Puts {@code player} in {@code shell}. The main body goes into the player file and the shell leaves the world. */
    public static Result link(ServerPlayer player, BodyHost shell) {
        return link(player, shell, null);
    }

    private static Result link(ServerPlayer player, BodyHost shell, @Nullable UUID seat) {
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
        if (refused(player, shell.getUUID())) {
            return Result.CANCELLED;
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
        // An old copy can be linked to when the outbox cannot be read, and the list must not go back with it.
        long epoch = Math.max(shell.epoch(), state.known(shell.getUUID()).map(LinkState.KnownShell::epoch).orElse(0L)) + 1;
        player.setData(ModAttachments.LINK, state.withoutOutbox(shell.getUUID()).withLink(new LinkState.Link(shell.getUUID(), epoch, homeTag, from, Optional.ofNullable(seat), player.server.overworld().getGameTime()))
                .withKnown(new LinkState.KnownShell(shell.getUUID(), epoch, place(shell), false, false)));
        if (!wear(player, body)) {
            boolean restored = wear(player, home);
            arrive(player, here, from.position(), from.yaw(), from.pitch());
            if (restored) {
                player.setData(ModAttachments.LINK, state);
            } else {
                // The link stays, so the main body is not lost, and so does the shell: it must not count as a copy left from before it.
                shell.setEpoch(epoch);
            }
            return Result.FAILED;
        }
        setHomeDust(player, homeDust);
        UUID id = shell.getUUID();
        shell.discard();
        settle(player);
        saveNow(player.server);
        tell(new ShellLinkEvent.Started(player, id));
        return Result.LINKED;
    }

    /** Brings {@code player} back to the main body, and puts the shell back in the world where they were. */
    public static Result wake(ServerPlayer player) {
        return wake(player, ShellLinkEvent.Reason.LEFT);
    }

    private static Result wake(ServerPlayer player, ShellLinkEvent.Reason why) {
        boolean lost = why == ShellLinkEvent.Reason.BODY_LOST;
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
        LinkState.Place point = wakePoint(player, link);
        if (point == null) {
            return Result.CANNOT_REACH;
        }
        ServerLevel target = player.server.getLevel(point.dimension());
        LinkState.Place at = place(player);
        ServerLevel shellLevel = player.serverLevel();
        BodySnapshot taken = BodySnapshot.take(player);
        // A lost shell keeps what it paid into the Replicator queue. They go in its snapshot, so they are in the outbox before the job
        // ends: a crash at any point leaves them in one place or the other. The job ends with the wake, below.
        ReplicatorLogic.Lost queued = lost ? Replicator.leftBy(player, Optional.of(link.shell())) : ReplicatorLogic.Lost.nothing(Replicator.data(player), player.getData(ModAttachments.DUST));
        BodySnapshot body = taken.withPending(queued.items());
        CompoundTag bodyTag = saved(body, player);
        if (bodyTag == null) {
            return Result.FAILED;
        }
        if (!arrive(player, target, point.position(), point.yaw(), point.pitch())) {
            return Result.CANNOT_REACH;
        }
        LinkState.Outbox outbox = new LinkState.Outbox(link.shell(), link.epoch(), bodyTag, at, false);
        player.setData(ModAttachments.LINK, state.withOutbox(outbox));
        Optional<BodyHost> shell = putBack(shellLevel, at, link, body, player);
        if (shell.isEmpty()) {
            LOGGER.warn("Shell {} could not be put back in the world yet: it stays in the outbox", link.shell());
        }
        if (!wear(player, charged(player, home.get(), link))) {
            shell.ifPresent(BodyHost::discard);
            wear(player, taken);
            arrive(player, shellLevel, at.position(), at.yaw(), at.pitch());
            player.setData(ModAttachments.LINK, state);
            return Result.FAILED;
        }
        shell.ifPresent(host -> {
            Outboxes.settled(host.getUUID());
            if (lost) {
                Wrecks.make(host);
            }
        });
        Replicator.settle(player, queued);
        // A lost shell that the level would not take comes back from the outbox as a shell, not a wreck.
        boolean wrecked = lost && shell.isPresent();
        player.setData(ModAttachments.LINK, state.withOutbox(outbox).withoutLink()
                .withKnown(new LinkState.KnownShell(link.shell(), link.epoch(), at, wrecked, false)));
        setHomeDust(player, DustData.NOT_LINKED);
        settle(player);
        saveNow(player.server);
        tell(new ShellLinkEvent.Ended(player, link.shell(), why));
        return Result.WOKEN;
    }

    /**
     * Where the main body wakes: where it was left, which a seat aboard a ship keeps current; else a listed Backup Core, with no
     * charge spent, because this is not a death; else the spawn. Only a dimension that is gone or locked gets past the first.
     */
    @Nullable
    private static LinkState.Place wakePoint(ServerPlayer player, LinkState.Link link) {
        if (reachable(player, link.from())) {
            return link.from();
        }
        for (LinkState.Place core : BackupCores.wakePlaces(player)) {
            if (reachable(player, core)) {
                return core;
            }
        }
        LinkState.Place spawn = new LinkState.Place(ModDimensions.DEEP_SPACE, SpawnHandler.spawnPosition(), 0f, 0f);
        return reachable(player, spawn) ? spawn : null;
    }

    /** The main body after its time in a seat. It is not in the world to tick, so this is worked out from the time it took. */
    private static BodySnapshot charged(ServerPlayer player, BodySnapshot home, LinkState.Link link) {
        if (link.seat().isEmpty()) {
            return home;
        }
        double perSecond = Config.LINK_SEAT_CHARGE.get() * (atStation(player.server, link.from()) ? Config.LINK_STATION_CHARGE.get() : 1.0);
        return home.withEnergy(SeatCharge.after(home.energy(), player.server.overworld().getGameTime() - link.since(), perSecond));
    }

    /** Whether a seat at {@code place} has a station's perks. */
    static boolean atStation(MinecraftServer server, LinkState.Place place) {
        ServerLevel level = server.getLevel(place.dimension());
        return level != null && level.getData(ModAttachments.STATIONS).nearest(place.position(), Config.STATION_RANGE.get()).isPresent();
    }

    private static boolean reachable(ServerPlayer player, LinkState.Place place) {
        ServerLevel level = player.server.getLevel(place.dimension());
        return level != null && mayEnter(player, level);
    }

    /** A seat aboard a ship has been carried along, and the main body with it. */
    static void homeCarriedTo(ServerPlayer player, Vec3 position) {
        LinkState state = state(player);
        state.link().ifPresent(link -> player.setData(ModAttachments.LINK, state.withLink(link.carriedTo(position))));
    }

    /** Whether an addon refuses this link. One that throws refuses it: nothing has changed yet, so that costs nothing. */
    private static boolean refused(ServerPlayer player, UUID shell) {
        try {
            return NeoForge.EVENT_BUS.post(new ShellLinkEvent.Pre(player, shell)).isCanceled();
        } catch (RuntimeException e) {
            LOGGER.error("A listener threw before {} could link", player.getScoreboardName(), e);
            return true;
        }
    }

    /** The move is done and saved: an addon that throws now must not take the server down with it. */
    private static void tell(ShellLinkEvent event) {
        try {
            NeoForge.EVENT_BUS.post(event);
        } catch (RuntimeException e) {
            LOGGER.error("A listener threw after a link changed", e);
        }
    }

    /** A throw is an addon's. The outbox already holds the shell, so a wake goes on without it, as when the level will not take it. */
    private static Optional<BodyHost> putBack(ServerLevel level, LinkState.Place at, LinkState.Link link, BodySnapshot body, ServerPlayer player) {
        return restored(level, at, link.shell(), link.epoch(), body, player);
    }

    /** A body back in the world, or empty if the level won't take it or an addon throws: it stays in the outbox then, and comes back from there. */
    private static Optional<BodyHost> restored(ServerLevel level, LinkState.Place at, UUID id, long epoch, BodySnapshot body, ServerPlayer player) {
        try {
            return BodyHost.restore(level, at, id, epoch, body, player.getUUID());
        } catch (RuntimeException e) {
            LOGGER.error("Body {} could not be put in the world", id, e);
            return Optional.empty();
        }
    }

    private static void setHomeDust(ServerPlayer player, float capacity) {
        player.setData(ModAttachments.HOME_DUST_CAPACITY, capacity);
        player.syncData(ModAttachments.HOME_DUST_CAPACITY);
    }

    /** What ends a move: the player's state on disk, in both places a server reads it from. */
    static void saveNow(MinecraftServer server) {
        server.getPlayerList().saveAll();
        // Singleplayer loads its owner from level.dat, which only an autosave or a quit writes, not the player file.
        CompoundTag host = server.getPlayerList().getSingleplayerData();
        if (host != null) {
            ((MinecraftServerStorageAccessor) server).void_reclaimer$storageSource().saveDataTag(server.registryAccess(), server.getWorldData(), host);
        }
    }

    /** A shell destroyed for good: nothing is to bring it back. An old copy is not the shell, so its end changes nothing. */
    static void destroyed(BodyHost shell) {
        ServerPlayer owner = owner(shell);
        if (owner != null) {
            DeathMarker.clear(owner, shell.getUUID());
        }
        if (owner != null && !older(state(owner), shell)) {
            LinkState state = state(owner);
            LinkState.KnownShell known = state.known(shell.getUUID()).orElseGet(() -> seen(shell));
            owner.setData(ModAttachments.LINK, state.withoutOutbox(shell.getUUID()).withKnown(known.destroyed()));
        }
    }

    /** Whether a newer link number than this copy's is on record: the link, the outbox or the list. */
    private static boolean older(LinkState state, BodyHost shell) {
        UUID id = shell.getUUID();
        return state.link().filter(link -> link.shell().equals(id) && link.epoch() > shell.epoch()).isPresent()
                || state.outbox(id).filter(outbox -> outbox.epoch() > shell.epoch()).isPresent()
                || state.known(id).filter(known -> known.epoch() > shell.epoch()).isPresent();
    }

    /** {@code shell} as it stands now. */
    static LinkState.KnownShell seen(BodyHost shell) {
        return new LinkState.KnownShell(shell.getUUID(), shell.epoch(), place(shell), shell.role() == HostRole.WRECK, false);
    }

    /** Puts {@code shell} on its owner's list, or brings its entry up to date. An old copy says nothing, and neither does a shell that is gone. */
    static void note(BodyHost shell) {
        ServerPlayer owner = owner(shell);
        if (owner != null) {
            LinkState state = state(owner);
            LinkState after = noted(state, shell);
            if (after != state) {
                owner.setData(ModAttachments.LINK, after);
            }
        }
    }

    /** {@code copy} stands refused, so its owner's list says where: if the newer copy is lost, the picker can offer this one. */
    static void noteOldCopy(BodyHost copy) {
        ServerPlayer owner = owner(copy);
        if (owner != null) {
            LinkState state = state(owner);
            Optional<LinkState.Place> here = Optional.of(place(copy));
            state.known(copy.getUUID()).filter(known -> !here.equals(known.oldCopy()))
                    .ifPresent(known -> owner.setData(ModAttachments.LINK, state.withKnown(known.withOldCopy(here))));
        }
    }

    /** A link looked where the list said, and the shell was not there. Recovery is offered only after this, so a healthy shell stays pickable. */
    static void noteMissing(ServerPlayer player, UUID shell) {
        LinkState state = state(player);
        state.known(shell).filter(known -> known.oldCopy().isPresent())
                .ifPresent(known -> player.setData(ModAttachments.LINK, state.withKnown(known.withMissing(true))));
    }

    static void forgetOldCopy(ServerPlayer player, UUID shell) {
        LinkState state = state(player);
        state.known(shell).ifPresent(known -> player.setData(ModAttachments.LINK, state.withKnown(known.withOldCopy(Optional.empty()).withMissing(false))));
    }

    /** {@code copy} is the shell now. A number above {@code newest} makes it so, and a lost copy that ever turns up gives way to it. */
    static void recover(ServerPlayer player, BodyHost copy, long newest) {
        copy.setEpoch(newest + 1);
        player.setData(ModAttachments.LINK, state(player).withKnown(seen(copy)));
        saveNow(player.server);
    }

    /** {@code state} with {@code shell} up to date, or {@code state} itself if that changes nothing. */
    static LinkState noted(LinkState state, BodyHost shell) {
        Optional<LinkState.KnownShell> known = state.known(shell.getUUID());
        LinkState.KnownShell now = seen(shell);
        if (known.filter(entry -> entry.gone() || entry.epoch() > shell.epoch() || entry.equals(now)).isPresent()) {
            return state;
        }
        return state.withKnown(now);
    }

    @Nullable
    private static ServerPlayer owner(BodyHost shell) {
        return shell.owner() == null ? null : shell.level().getServer().getPlayerList().getPlayer(shell.owner());
    }

    /**
     * Whether this copy of a shell is left over, so it takes no link and gives up nothing: {@link #replaced}, or the outbox has ended
     * and the list says the shell is newer than this. Only a crash leaves such a copy, on disk from before the link. An outbox nobody
     * can read decides alone, because this copy is then the only readable one. With its owner away nobody can tell, and nobody can
     * use it either.
     */
    public static boolean stale(BodyHost shell) {
        ServerPlayer owner = owner(shell);
        if (owner == null) {
            return false;
        }
        LinkState state = state(owner);
        UUID id = shell.getUUID();
        return replaced(owner, state, shell)
                || state.outbox(id).isEmpty() && state.known(id).filter(known -> known.epoch() > shell.epoch()).isPresent();
    }

    /**
     * Whether this copy may remove itself: the newer body is known to exist, or the shell is gone. A list that only says a newer
     * number was seen does not prove that copy still exists: the level refuses a copy it cannot see beside one in an unloading chunk,
     * and that copy is lost at its chunk's next save. So such a copy stays, refused, and gives way when the newer one turns up in its
     * level ({@link #onEntityJoin}). The worst case is then a rollback, not an empty world.
     */
    public static boolean replaced(BodyHost shell) {
        ServerPlayer owner = owner(shell);
        return owner != null && replaced(owner, state(owner), shell);
    }

    private static boolean replaced(ServerPlayer owner, LinkState state, BodyHost shell) {
        UUID id = shell.getUUID();
        if (state.link().filter(link -> link.shell().equals(id) && link.epoch() > shell.epoch()).isPresent()) {
            return true;
        }
        Optional<LinkState.Outbox> outbox = state.outbox(id);
        if (outbox.isPresent()) {
            return outbox.get().epoch() > shell.epoch() && BodySnapshot.load(outbox.get().body(), owner.registryAccess()).isPresent();
        }
        return state.known(id).filter(LinkState.KnownShell::gone).isPresent();
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

    static LinkState.Place place(Entity entity) {
        return new LinkState.Place(entity.level().dimension(), entity.position(), entity.getYRot(), entity.getXRot());
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
