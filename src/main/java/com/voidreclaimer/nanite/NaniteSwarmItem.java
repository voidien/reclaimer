package com.voidreclaimer.nanite;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Nanite Swarm: right-click to build the offhand block (or Scaffold) in empty space ahead. Sneak + scroll changes
 * the distance. Looking at a block within reach builds against its face instead, like placing by hand.
 */
public final class NaniteSwarmItem extends Item {
    public NaniteSwarmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer && Nanites.build(serverPlayer, Nanites.range(stack)) == Nanites.Result.BUILT) {
            player.getCooldowns().addCooldown(this, Nanites.BUILD_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Looking at a block within reach: build against the clicked face, like placing by hand. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() instanceof ServerPlayer player
                && Nanites.buildAt(player, context.getClickedPos().relative(context.getClickedFace()), context.getClickedFace()) == Nanites.Result.BUILT) {
            player.getCooldowns().addCooldown(this, Nanites.BUILD_TICKS);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.void_reclaimer.nanite_swarm.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.void_reclaimer.nanite_swarm.tooltip.range", Nanites.range(stack))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
