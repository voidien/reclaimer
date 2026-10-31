package com.voidreclaimer.helper;

import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Helper Drone as an item. Sneak-use on an inventory block sets the drone's source, then its destination (a third
 * sneak-use on an inventory starts over with a new source); sneak-use on asteroid rock sets a mine point instead of a
 * source. Use on a block or in the air puts the drone down there with those links.
 */
public class HelperDroneItem extends Item {
    /** How far ahead a drone is released into empty space. */
    public static final double AHEAD = 3.0;

    public HelperDroneItem(Properties properties) {
        super(properties);
    }

    public static HelperLinks links(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.HELPER_LINKS.get(), HelperLinks.EMPTY);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (player != null && player.isShiftKeyDown()) {
            boolean rock = level.getBlockState(pos).is(ModBlocks.ASTEROID_BLOCKS);
            boolean container = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
            if (!rock && !container) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.void_reclaimer.helper.not_a_container"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                HelperLinks links = links(stack);
                String key;
                if (rock) {
                    links = new HelperLinks(java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.of(pos.immutable()));
                    key = "message.void_reclaimer.helper.mine_set";
                } else if ((links.source().isPresent() || links.mining()) && links.destination().isEmpty()) {
                    links = links.withDestination(pos);
                    key = "message.void_reclaimer.helper.destination_set";
                } else {
                    links = new HelperLinks(java.util.Optional.of(pos.immutable()), java.util.Optional.empty());
                    key = "message.void_reclaimer.helper.source_set";
                }
                stack.set(ModDataComponents.HELPER_LINKS.get(), links);
                player.displayClientMessage(Component.translatable(key, pos.getX(), pos.getY(), pos.getZ()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel server) {
            release(server, context.getClickLocation().add(0, HelperDrone.HOVER, 0), stack, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel server) {
            release(server, player.getEyePosition().add(player.getLookAngle().scale(AHEAD)), stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void release(ServerLevel level, Vec3 where, ItemStack stack, Player player) {
        HelperDrone.spawn(level, where, links(stack), player == null ? null : player.getUUID());
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HelperLinks links = links(stack);
        if (links.mining()) {
            tooltip.add(link("tooltip.void_reclaimer.helper.mine", links.mine().orElse(null)));
        } else {
            tooltip.add(link("tooltip.void_reclaimer.helper.source", links.source().orElse(null)));
        }
        tooltip.add(link("tooltip.void_reclaimer.helper.destination", links.destination().orElse(null)));
    }

    private static Component link(String key, BlockPos pos) {
        Component where = pos == null ? Component.translatable("tooltip.void_reclaimer.helper.none")
                : Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ());
        return Component.translatable(key, where).withStyle(ChatFormatting.GRAY);
    }
}
