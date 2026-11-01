package com.voidreclaimer.mixin;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test infrastructure only: logs how long each GameTest ran, so {@code tools/gametest.sh} can name the slowest. A batch lasts
 * as long as its slowest test, and nothing else says which one that is.
 */
@Mixin(GameTestInfo.class)
abstract class GameTestTimeMixin {
    @Shadow
    public abstract long getRunTime();

    @Shadow
    public abstract String getTestName();

    @Shadow
    abstract long getTick();

    @Inject(method = "finish", at = @At("TAIL"))
    private void void_reclaimer$logTime(CallbackInfo ci) {
        VoidReclaimer.LOGGER.info("VR_TEST_TIME {} ms {} ticks {}", getRunTime(), getTick(), getTestName());
    }
}
