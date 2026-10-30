package com.voidreclaimer.earth;

import com.voidreclaimer.registry.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Right-click a frame block to light the Atmospheric Portal it encloses. Used up by lighting (not in creative). */
public class PortalIgniterItem extends Item {
    public PortalIgniterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(ModBlocks.PORTAL_FRAME.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!AtmosphericPortals.tryLight(level, context.getClickedPos())) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, context.getClickedPos(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 0.8f);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
