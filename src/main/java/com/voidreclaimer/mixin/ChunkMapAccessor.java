package com.voidreclaimer.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.util.thread.ProcessorHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test infrastructure only: the chunk map's task queues, for {@link GameTestChunkTasksMixin} and its GameTest. */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Accessor("queueSorter")
    ChunkTaskPriorityQueueSorter void_reclaimer$queueSorter();

    @Accessor("mainThreadMailbox")
    ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> void_reclaimer$mainThreadMailbox();
}
