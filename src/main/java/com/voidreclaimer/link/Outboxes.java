package com.voidreclaimer.link;

import com.mojang.logging.LogUtils;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Keeps the promise of {@link LinkState.Outbox}: a shell put back in the world reaches the disk, or it is built again.
 *
 * <p>While a player online has an outbox, its chunk is held loaded for a look once a second, and a shell that is not there, or
 * only as an older copy, is rebuilt from the outbox. The copy goes once the shell has been through two saves of its level: a save
 * only hands entities to the IO thread, so one is not proof that they are on disk.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Outboxes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EVERY_TICKS = 20;
    /** Ends by itself, so a player who leaves mid-look leaves nothing loaded. */
    private static final TicketType<ChunkPos> LOOK = TicketType.create("void_reclaimer_outbox", Comparator.comparingLong(ChunkPos::toLong), 3 * EVERY_TICKS);
    /** Entity-ticking in the middle: {@link ServerLevel#areEntitiesLoaded} needs the chunk's entities read. */
    private static final int LOOK_RADIUS = 2;

    private Outboxes() {}

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % EVERY_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                check(player);
            }
        }
    }

    @SubscribeEvent
    static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                saved(player, level);
            }
        }
    }

    private static void check(ServerPlayer player) {
        for (LinkState.Outbox outbox : Links.state(player).outboxes()) {
            ServerLevel level = player.server.getLevel(outbox.at().dimension());
            if (level == null) {
                continue;
            }
            ChunkPos chunk = new ChunkPos(BlockPos.containing(outbox.at().position()));
            if (!level.areEntitiesLoaded(chunk.toLong())) {
                level.getChunkSource().addRegionTicket(LOOK, chunk, LOOK_RADIUS, chunk);
            } else if (!there(level, outbox)) {
                rebuild(player, level, outbox);
            }
        }
    }

    private static boolean there(ServerLevel level, LinkState.Outbox outbox) {
        return level.getEntity(outbox.shell()) instanceof BodyHost shell && shell.epoch() >= outbox.epoch();
    }

    private static void rebuild(ServerPlayer player, ServerLevel level, LinkState.Outbox outbox) {
        Optional<BodySnapshot> body = BodySnapshot.load(outbox.body(), player.registryAccess());
        if (body.isEmpty()) {
            LOGGER.error("The copy of shell {} cannot be read: it stays in the outbox", outbox.shell());
            return;
        }
        if (BodyHost.restore(level, outbox.at(), outbox.shell(), outbox.epoch(), body.get(), player.getUUID()).isPresent()) {
            LOGGER.info("Shell {} was missing and has been rebuilt from the outbox", outbox.shell());
        }
    }

    private static void saved(ServerPlayer player, ServerLevel level) {
        LinkState state = Links.state(player);
        LinkState after = state;
        for (LinkState.Outbox outbox : state.outboxes()) {
            if (outbox.at().dimension() == level.dimension() && there(level, outbox)) {
                after = outbox.saved() ? after.withoutOutbox(outbox.shell()) : after.withOutbox(outbox.seenInASave());
            }
        }
        if (after != state) {
            player.setData(ModAttachments.LINK, after);
        }
    }
}
