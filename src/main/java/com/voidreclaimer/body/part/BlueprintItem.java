package com.voidreclaimer.body.part;

import com.voidreclaimer.body.Blueprint;
import com.voidreclaimer.body.Blueprints;
import com.voidreclaimer.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A saved body design. Use an empty one (or sneak-use) to save the current body; use a saved one to build it from the
 * inventory. The Body tab offers the same, plus queueing the missing parts in the Replicator.
 */
public class BlueprintItem extends Item {
    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            Blueprint blueprint = stack.get(ModDataComponents.BLUEPRINT.get());
            if (blueprint == null || player.isShiftKeyDown()) {
                Blueprints.save(serverPlayer, stack);
            } else {
                Blueprints.apply(serverPlayer, blueprint);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.BLUEPRINT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Blueprint blueprint = stack.get(ModDataComponents.BLUEPRINT.get());
        if (blueprint == null) {
            tooltip.add(Component.translatable("item.void_reclaimer.body_blueprint.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.void_reclaimer.body_blueprint.saved", blueprint.core().getDescription(),
                blueprint.sections().size(), blueprint.modules().size()).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.void_reclaimer.body_blueprint.usage").withStyle(ChatFormatting.GRAY));
    }
}
