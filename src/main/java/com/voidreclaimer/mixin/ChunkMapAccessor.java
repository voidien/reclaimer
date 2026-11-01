package com.voidreclaimer.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.util.thread.ProcessorHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Debug and test tools only: the chunk map's task queues and holders, for {@link GameTestChunkTasksMixin} and {@code debug.ChunkReadout}. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Accessor("queueSorter")
    ChunkTaskPriorityQueueSorter void_reclaimer$queueSorter();

    @Accessor("mainThreadMailbox")
    ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> void_reclaimer$mainThreadMailbox();

    @Invoker("getChunks")
    Iterable<ChunkHolder> void_reclaimer$chunks();
}
