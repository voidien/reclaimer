package com.voidreclaimer.link;

import com.voidreclaimer.VoidReclaimer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Brings back an old copy of a shell whose newer copy is lost. Only a crash can leave that (see {@link Links#replaced}): the old copy
 * then stands refusing links, and the picker offers it once a link has found nothing where the list said. The newer copy's chunks
 * load first, and a shell found there is left alone. Then the old copy takes a number above the list's, which makes it the shell.
 *
 * <p>A newer copy that had moved from where the list says cannot be found this way. If it loads later it loses to the recovered one
 * and is gone: the owner's choice, and the picker's confirmation says so.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ShellRecovery {
    private static final int LIMIT_TICKS = 200;

    /** How a recovery ended. */
    public enum Result {
        RECOVERED,
        /** The newer copy is there after all: nothing changed. */
        STILL_THERE,
        /** No old copy stands where it was seen. */
        NOT_FOUND,
        TIMED_OUT,
        /** Nothing on the list to recover, or the player is linked, or a recovery is on its way. */
        REFUSED,
        PLAYER_GONE
    }

    /** {@code newestRead} is whether the newer copy's chunks have been looked through. */
    private record Waiting(UUID player, UUID shell, long epoch, LinkState.Place newest, LinkState.Place old, boolean newestRead, long until, Consumer<Result> then) {
        ServerLevel newestLevel(MinecraftServer server) {
            return server.getLevel(newest.dimension());
        }

        /** The second load gets its own time: a slow first one must not use it up. */
        Waiting read(long tick) {
            return new Waiting(player, shell, epoch, newest, old, true, tick + LIMIT_TICKS, then);
        }
    }

    /** At most one for each player. Lives on the server thread. */
    private static final List<Waiting> WAITING = new ArrayList<>();

    private ShellRecovery() {}

    /** Starts a recovery; {@code then} hears how it ended. Nothing changes unless it ends {@link Result#RECOVERED}. */
    public static void request(ServerPlayer player, UUID shell, Consumer<Result> then) {
        LinkState state = Links.state(player);
        Optional<LinkState.KnownShell> known = state.known(shell).filter(LinkState.KnownShell::recoverable);
        boolean waiting = WAITING.stream().anyMatch(entry -> entry.player().equals(player.getUUID()));
        if (known.isEmpty() || state.linked() || state.outbox(shell).isPresent() || waiting) {
            then.accept(Result.REFUSED);
            return;
        }
        LinkState.KnownShell entry = known.get();
        long until = player.server.getTickCount() + (long) LIMIT_TICKS;
        WAITING.add(new Waiting(player.getUUID(), shell, entry.epoch(), entry.at(), entry.oldCopy().get(), false, until, then));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        // A copy: each step may replace or end its entry.
        for (Waiting waiting : List.copyOf(WAITING)) {
            step(event.getServer(), waiting);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        WAITING.clear();
    }

    private static void step(MinecraftServer server, Waiting waiting) {
        ServerPlayer player = server.getPlayerList().getPlayer(waiting.player());
        if (player == null) {
            end(waiting, Result.PLAYER_GONE);
            return;
        }
        LinkState.Place place = waiting.newestRead() ? waiting.old() : waiting.newest();
        ServerLevel level = server.getLevel(place.dimension());
        if (level == null) {
            // A dimension that has gone holds no newer copy, and no old one.
            if (waiting.newestRead()) {
                forget(player, waiting, Result.NOT_FOUND);
            } else {
                WAITING.set(WAITING.indexOf(waiting), waiting.read(server.getTickCount()));
            }
            return;
        }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(place.position()));
        if (!ShellReach.read(level, chunk)) {
            if (server.getTickCount() >= waiting.until()) {
                end(waiting, Result.TIMED_OUT);
            } else {
                ShellReach.hold(level, chunk);
            }
            return;
        }
        BodyHost found = level.getEntity(waiting.shell()) instanceof BodyHost host ? host : null;
        if (!waiting.newestRead()) {
            if (found != null && found.epoch() >= waiting.epoch()) {
                discardOldCopy(server, waiting);
                forget(player, waiting, Result.STILL_THERE);
            } else {
                WAITING.set(WAITING.indexOf(waiting), waiting.read(server.getTickCount()));
            }
        } else if (found != null && found.epoch() < waiting.epoch()) {
            if (stillSo(player, waiting)) {
                Links.recover(player, found, waiting.epoch());
                end(waiting, Result.RECOVERED);
            } else {
                end(waiting, Result.REFUSED);
            }
        } else {
            forget(player, waiting, Result.NOT_FOUND);
        }
    }

    /** The wait can run for seconds: a link, a wake or a destroyed shell in that time ends it, or two live bodies could share a number. */
    private static boolean stillSo(ServerPlayer player, Waiting waiting) {
        LinkState state = Links.state(player);
        return !state.linked() && state.outbox(waiting.shell()).isEmpty()
                && state.known(waiting.shell()).filter(known -> known.recoverable() && known.epoch() == waiting.epoch()).isPresent();
    }

    /** The newer body is proven to exist, so a loaded old copy in another level is only that. */
    private static void discardOldCopy(MinecraftServer server, Waiting waiting) {
        ServerLevel level = server.getLevel(waiting.old().dimension());
        if (level != waiting.newestLevel(server) && level != null && level.getEntity(waiting.shell()) instanceof BodyHost old && old.epoch() < waiting.epoch()) {
            old.discard();
        }
    }

    /** The list's word about an old copy is wrong or spent. */
    private static void forget(ServerPlayer player, Waiting waiting, Result result) {
        Links.forgetOldCopy(player, waiting.shell());
        end(waiting, result);
    }

    private static void end(Waiting waiting, Result result) {
        WAITING.remove(waiting);
        waiting.then().accept(result);
    }
}
