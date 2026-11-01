package com.voidreclaimer.link;

import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.registry.ModDataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A Remote Shell, folded up. Use sets it down ahead of the player, owned by them, wearing the basic body of the chassis in its
 * {@code shell_body} component. It then lies inert until it is linked to from an Uplink Seat.
 */
public class RemoteShellItem extends Item {
    public RemoteShellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            ResourceLocation chassis = stack.getOrDefault(ModDataComponents.SHELL_BODY.get(), BuiltinBodies.DRONE.location());
            BodyHost.shellAhead(server, Bodies.preset(ResourceKey.create(BuiltinBodies.DRONE.registryKey(), chassis)));
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
