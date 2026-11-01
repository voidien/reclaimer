package com.voidreclaimer.mixin;

import com.voidreclaimer.gametest.GameTestFilter;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test infrastructure only: each test forces chunks of its own, and each forced area keeps about 625 half-made chunks of 254 sections
 * in memory. Held for a whole run, that outgrew a 10 GB heap at about 400 tests. Every test in a batch has finished when the next
 * batch starts, so nothing needs them then. This also clears what an earlier run left forced in a local world. Only a run that writes
 * no chunks: otherwise each one is saved as it unloads, and their data piles up in memory faster than the disk takes it.
 */
@Mixin(GameTestRunner.class)
abstract class GameTestBatchChunkMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "runBatch", at = @At("HEAD"))
    private void void_reclaimer$release(int index, CallbackInfo ci) {
        if (GameTestFilter.skipsChunkSaves()) {
            for (ServerLevel each : level.getServer().getAllLevels()) {
                for (long chunk : new LongArrayList(each.getForcedChunks())) {
                    each.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
                }
            }
        }
    }
}
