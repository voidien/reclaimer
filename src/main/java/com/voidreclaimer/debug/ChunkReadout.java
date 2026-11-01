package com.voidreclaimer.debug;

import com.voidreclaimer.mixin.ChunkMapAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Why a chunk is or is not ticking: its own state, and how much chunk work is waiting. The waiting count is what explained
 * forced chunks that seemed never to tick: they were not stuck, only behind a long queue. {@code /void_reclaimer debug chunks}.
 */
public final class ChunkReadout {
    private ChunkReadout() {}

    /** The chunk at {@code pos} in {@code level}, a line each. */
    public static List<String> of(ServerLevel level, ChunkPos pos) {
        ServerChunkCache cache = level.getChunkSource();
        ChunkMapAccessor chunks = (ChunkMapAccessor) cache.chunkMap;
        List<String> lines = new ArrayList<>();
        lines.add("Chunk " + pos.x + " " + pos.z + " in " + level.dimension().location());
        String data = cache.getChunkDebugData(pos);
        lines.add("null".equals(data) ? "Not loaded" : "Level " + plain(data));
        lines.add("Entities: " + (level.areEntitiesLoaded(pos.toLong()) ? "loaded" : "not loaded") + ", "
                + (level.isPositionEntityTicking(pos.getWorldPosition()) ? "ticking" : "not ticking"));
        int held = 0;
        int finished = 0;
        int waiting = 0;
        for (ChunkHolder holder : chunks.void_reclaimer$chunks()) {
            held++;
            boolean full = holder.getLatestStatus() == ChunkStatus.FULL;
            finished += full ? 1 : 0;
            waiting += !full && ChunkLevel.fullStatus(holder.getTicketLevel()).isOrAfter(FullChunkStatus.FULL) ? 1 : 0;
        }
        lines.add("Chunks here: " + held + " held, " + finished + " finished, " + waiting + " waiting to finish");
        lines.add("Queue: " + (chunks.void_reclaimer$queueSorter().hasWork() ? "working" : "empty") + ", "
                + cache.getPendingTasksCount() + " tasks for the main thread");
        return lines;
    }

    /** The game's debug text with its formatting codes and line breaks taken out. */
    static String plain(String data) {
        return data.replaceAll("§.", "").strip().replace('\n', ' ');
    }
}
