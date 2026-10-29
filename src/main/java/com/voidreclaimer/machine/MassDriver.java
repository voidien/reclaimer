package com.voidreclaimer.machine;

import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Kinetic Mass Driver structure: the Kinetic Drive Base, the Payload Chamber on top of it and the Barrel Nozzle on top of that. */
public final class MassDriver {
    private MassDriver() {}

    public static BlockPos chamberPos(BlockPos base) {
        return base.above();
    }

    public static BlockPos nozzlePos(BlockPos base) {
        return base.above(2);
    }

    /** Whether the chamber and the nozzle stand on the base at {@code base}. */
    public static boolean isAssembled(Level level, BlockPos base) {
        return level.getBlockState(chamberPos(base)).is(ModBlocks.PAYLOAD_CHAMBER.get())
                && level.getBlockState(nozzlePos(base)).is(ModBlocks.MAGNETIC_BARREL_NOZZLE.get());
    }

    public static boolean isSatellite(ItemStack stack) {
        return stack.is(ModTags.DYSON_SATELLITE_PAYLOADS);
    }

    public static boolean isTruss(ItemStack stack) {
        return stack.is(ModTags.DYSON_TRUSS_PAYLOADS);
    }

    /** Whether the chamber accepts the item at all. */
    public static boolean isPayload(ItemStack stack) {
        return isSatellite(stack) || isTruss(stack);
    }
}
