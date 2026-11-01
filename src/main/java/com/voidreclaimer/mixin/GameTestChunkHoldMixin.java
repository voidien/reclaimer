package com.voidreclaimer.mixin;

import com.voidreclaimer.gametest.GameTestFilter;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test infrastructure only: most of a GameTest run went into saving chunks, which are 254 sections tall in deep space, as they
 * dropped out of use, and loading them again for the next test. {@link GameTestFilter#holdsChunks()} runs keep every chunk in
 * memory. A chunk whose tickets go still changes status, so its entities and ships unload as usual; only the write to disk and
 * the read back are skipped, and CI runs those for real on every push to main.
 */
@Mixin(ChunkMap.class)
abstract class GameTestChunkHoldMixin {
    @Inject(method = "processUnloads", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$hold(BooleanSupplier hasMoreTime, CallbackInfo ci) {
        if (GameTestFilter.holdsChunks()) {
            ci.cancel();
        }
    }
}
