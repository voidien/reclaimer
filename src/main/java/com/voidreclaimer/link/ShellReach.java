package com.voidreclaimer.link;

import com.voidreclaimer.VoidReclaimer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A link to a shell that may be somewhere nothing is loaded. The player's list says where the shell was last seen, so its chunks
 * load first and the move is asked for when the shell turns up. Nothing changes before that: a shell that is not there, or a chunk
 * that will not load, ends the wait and nothing else.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ShellReach {
    /** Long enough for a busy server to load nine chunks, short enough that the player is not left looking at nothing. */
    private static final int LIMIT_TICKS = 200;
    /** Ends by itself, so a wait that is dropped leaves nothing loaded. */
    private static final TicketType<ChunkPos> REACH = TicketType.create("void_reclaimer_reach", Comparator.comparingLong(ChunkPos::toLong), 40);
    /** Three chunks by three: a shell can sit on a chunk's edge, and the list only knows where it was at the last save. */
    private static final int RADIUS = 1;

    private record Waiting(UUID player, UUID shell, @Nullable UUID seat, ResourceKey<Level> dimension, ChunkPos chunk, long until, Consumer<Links.Result> then) {}

    /** At most one for each player. Lives on the server thread. */
    private static final List<Waiting> WAITING = new ArrayList<>();

    private ShellReach() {}

    static void request(ServerPlayer player, UUID shell, Consumer<Links.Result> then) {
        request(player, shell, null, LIMIT_TICKS, then);
    }

    static void request(ServerPlayer player, UUID shell, UUID seat, Consumer<Links.Result> then) {
        request(player, shell, seat, LIMIT_TICKS, then);
    }

    /** {@link #request} with a limit of a test's choosing. */
    public static void request(ServerPlayer player, UUID shell, int limitTicks, Consumer<Links.Result> then) {
        request(player, shell, null, limitTicks, then);
    }

    private static void request(ServerPlayer player, UUID shell, @Nullable UUID seat, int limitTicks, Consumer<Links.Result> then) {
        LinkState state = Links.state(player);
        Optional<LinkState.KnownShell> known = state.known(shell).filter(entry -> !entry.gone() && !entry.wrecked());
        if (state.linked()) {
            then.accept(Links.Result.ALREADY_LINKED);
        } else if (known.isEmpty()) {
            then.accept(Links.Result.NOT_A_SHELL);
        } else if (WAITING.stream().anyMatch(waiting -> waiting.player().equals(player.getUUID()))) {
            then.accept(Links.Result.WAITING);
        } else {
            LinkState.Place at = known.get().at();
            ChunkPos chunk = new ChunkPos(BlockPos.containing(at.position()));
            WAITING.add(new Waiting(player.getUUID(), shell, seat, at.dimension(), chunk, player.server.getTickCount() + (long) limitTicks, then));
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        // A copy: a listener that hears the end of a wait may ask for another.
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
        ServerLevel level = server.getLevel(waiting.dimension());
        if (player == null) {
            end(waiting, Links.Result.PLAYER_GONE);
        } else if (level == null) {
            end(waiting, Links.Result.CANNOT_REACH);
        } else if (level.getEntity(waiting.shell()) instanceof BodyHost shell) {
            WAITING.remove(waiting);
            Links.requestLink(player, shell, waiting.seat(), waiting.then());
        } else if (read(level, waiting.chunk())) {
            end(waiting, Links.Result.NOT_FOUND);
        } else if (server.getTickCount() >= waiting.until()) {
            end(waiting, Links.Result.TIMED_OUT);
        } else {
            level.getChunkSource().addRegionTicket(REACH, waiting.chunk(), RADIUS, waiting.chunk());
        }
    }

    private static void end(Waiting waiting, Links.Result result) {
        WAITING.remove(waiting);
        waiting.then().accept(result);
    }

    private static boolean read(ServerLevel level, ChunkPos middle) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (!level.areEntitiesLoaded(ChunkPos.asLong(middle.x + x, middle.z + z))) {
                    return false;
                }
            }
        }
        return true;
    }
}
