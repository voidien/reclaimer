package com.voidreclaimer.mixin;

import java.util.concurrent.locks.LockSupport;
import net.minecraft.Util;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test infrastructure only: a normal server waits between ticks and works through the chunk tasks while it waits. The
 * GameTest server does not wait: it runs what is queued at that moment and starts the next tick. The chunk queue hands
 * the main thread one chunk at a time, from another thread, so the queue looks empty right after every task and the
 * GameTest server finishes exactly one chunk per tick. A chunk needs its 5 x 5 at full status to tick entities, so 200
 * forced chunks restored from an earlier run took thousands of ticks, far past any test's timeout. Here the GameTest
 * server keeps working on chunk tasks for up to one normal tick's time while the queue has work.
 */
@Mixin(GameTestServer.class)
abstract class GameTestChunkTasksMixin {
    /** One tick of a normal server. */
    @Unique
    private static final long void_reclaimer$PUMP_NANOS = 50_000_000L;
    /** The nap a normal server takes between looks at its queue ({@code BlockableEventLoop.waitForTasks}). */
    @Unique
    private static final long void_reclaimer$NAP_NANOS = 100_000L;

    @Inject(method = "waitUntilNextTick", at = @At("TAIL"))
    private void void_reclaimer$workThroughChunkTasks(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        long deadline = Util.getNanos() + void_reclaimer$PUMP_NANOS;
        while (Util.getNanos() < deadline) {
            boolean ran = false;
            boolean queued = false;
            for (ServerLevel level : server.getAllLevels()) {
                ran |= level.getChunkSource().pollTask();
                queued |= ((ChunkMapAccessor) level.getChunkSource().chunkMap).void_reclaimer$queueSorter().hasWork();
            }
            if (!ran && !queued) {
                return;
            }
            if (!ran) {
                LockSupport.parkNanos(void_reclaimer$NAP_NANOS);
            }
        }
    }
}
