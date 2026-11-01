package com.voidreclaimer.mixin;

import com.voidreclaimer.gametest.GameTestFilter;
import java.util.function.BooleanSupplier;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.MultipleTestTracker;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test infrastructure only: a {@link GameTestFilter#skipsChunkSaves()} run has nothing to save, so it ends right after its summary,
 * with the same exit code a normal stop gives. Restart tests and CI's runs on main stop normally.
 */
@Mixin(GameTestServer.class)
abstract class GameTestQuickExitMixin {
    @Shadow
    private MultipleTestTracker testTracker;

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void void_reclaimer$exitAfterTheSummary(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (GameTestFilter.skipsChunkSaves() && testTracker.isDone()) {
            LogManager.shutdown();
            Runtime.getRuntime().halt(testTracker.getFailedRequiredCount());
        }
    }
}
