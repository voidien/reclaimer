package com.voidreclaimer.mixin;

import com.voidreclaimer.gametest.GameTestFilter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Test infrastructure only: most of a GameTest run went into writing chunks, which are 254 sections tall in deep space. A
 * {@link GameTestFilter#skipsChunkSaves()} run writes none. Chunks still load and unload, but one that unloads is gone: it comes back
 * as the generator made it. A test that needs a chunk must hold a ticket on it, and a test that needs the disk belongs in a restart
 * test; those run with real saves, and CI runs real saves on every push to main.
 */
@Mixin(ChunkMap.class)
abstract class GameTestChunkSaveMixin {
    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$skip(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (GameTestFilter.skipsChunkSaves()) {
            cir.setReturnValue(false);
        }
    }
}
