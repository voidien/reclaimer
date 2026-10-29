package com.voidreclaimer.starship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Creative spawner for the {@link TestStarship}: on a block, or {@link #AHEAD} blocks ahead in empty space. */
public class TestStarshipItem extends Item {
    /** Far enough that a Ship-sized hull doesn't swallow the player. */
    public static final double AHEAD = 12.0;

    public TestStarshipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level) {
            TestStarship.spawn(level, context.getClickLocation());
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            TestStarship.spawn(server, ahead(player.getEyePosition(), player.getLookAngle()));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static Vec3 ahead(Vec3 eye, Vec3 look) {
        return eye.add(look.scale(AHEAD));
    }
}
