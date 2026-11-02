package com.voidreclaimer.mixin;

import com.voidreclaimer.ship.ShipCrash;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

/** Every block answers Sable's collision callback, so a ship hears what it hits. A block Sable gives a callback of its own keeps that one. */
@Mixin(Block.class)
abstract class BlockCrashMixin implements BlockWithSubLevelCollisionCallback {
    @Override
    public BlockSubLevelCollisionCallback sable$getCallback() {
        return ShipCrash.INSTANCE;
    }
}
