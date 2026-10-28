package com.voidreclaimer.mixin;

import com.voidreclaimer.space.ZeroGravity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sand, gravel, concrete powder, anvils etc. stay where they are placed in zero gravity. */
@Mixin(FallingBlock.class)
abstract class FallingBlockMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$holdInZeroG(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (ZeroGravity.isZeroG(level, pos)) {
            ci.cancel();
        }
    }
}
