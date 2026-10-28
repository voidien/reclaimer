package com.voidreclaimer.salvage;

import com.voidreclaimer.registry.ModBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dismantles things the AI built ({@code #void_reclaimer:salvageable}) quickly and refunds part of their dust
 * instead of dropping the block. It is not a drill: on anything else it is no faster than a bare hand.
 */
public final class SalvageToolItem extends Item {
    private static final float SALVAGE_SPEED = 20f;

    public SalvageToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(ModBlocks.SALVAGEABLE) ? SALVAGE_SPEED : 1f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.void_reclaimer.salvage_tool.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
