package com.voidreclaimer.machine;

import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Spout casting: a Spout over an <em>empty</em> Depot casts Liquid Silicon into a Quartz Wafer. Create's own spouts only fill
 * an item that is already there; this adds the "new item from fluid" case for the Depot. With an item on the Depot this
 * does nothing, so normal spout filling (and Sequenced Assembly) is untouched.
 */
public final class SpoutCasting implements BlockSpoutingBehaviour {
    @Override
    public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack available, boolean simulate) {
        int amount = Config.CASTING_AMOUNT.get();
        if (!Config.isEnabled("spoutCasting") || !available.is(ModFluids.LIQUID_SILICON.source().get()) || available.getAmount() < amount) {
            return 0;
        }
        if (!(level.getBlockEntity(pos) instanceof DepotBlockEntity depot)) {
            return 0;
        }
        DepotBehaviour behaviour = depot.getBehaviour(DepotBehaviour.TYPE);
        if (behaviour == null || !behaviour.isEmpty() || !behaviour.isOutputEmpty()) {
            return 0;
        }
        if (!simulate) {
            depot.setHeldItem(new ItemStack(ModMaterials.QUARTZ_WAFER.get()));
        }
        return amount;
    }
}
