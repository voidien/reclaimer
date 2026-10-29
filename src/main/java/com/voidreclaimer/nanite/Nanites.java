package com.voidreclaimer.nanite;

import com.voidreclaimer.Config;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModSounds;
import com.voidreclaimer.replicator.Replicator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Nanite Swarm's building logic, run from the tool module ({@link com.voidreclaimer.body.Modules}).
 *
 * <p>The swarm assembles a block in empty space {@link NaniteTargeting range} blocks ahead: the offhand block if
 * there is one, otherwise Scaffold from the inventory. Placement goes through the normal {@link BlockItem#place}
 * path, so block states, block entities and item use behave as if placed by hand; only the "needs a neighbor"
 * rule is skipped.
 */
public final class Nanites {
    /** Ticks between builds (the module's cooldown). */
    public static final int BUILD_TICKS = 10;

    private Nanites() {}

    public enum Result { BUILT, NO_POWER, NO_BLOCKS, BLOCKED }

    public static int range(ItemStack tool) {
        return NaniteTargeting.clampRange(tool.getOrDefault(ModDataComponents.NANITE_RANGE.get(), NaniteTargeting.DEFAULT_RANGE));
    }

    public static BlockPos target(Player player, int range) {
        return NaniteTargeting.target(player.getEyePosition(), player.getLookAngle(), range);
    }

    /** The block stack the swarm would use: the offhand block, otherwise the first Scaffold in the inventory. */
    @Nullable
    public static Source source(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof BlockItem) {
            return new Source(offhand, InteractionHand.OFF_HAND);
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.SCAFFOLD.get())) {
                return new Source(stack, InteractionHand.MAIN_HAND);
            }
        }
        return null;
    }

    /** Builds in empty space {@code range} blocks ahead. */
    public static Result build(ServerPlayer player, int range) {
        BlockPos pos = target(player, range);
        Vec3 eyes = player.getEyePosition();
        Direction face = Direction.getNearest(eyes.x - (pos.getX() + 0.5), eyes.y - (pos.getY() + 0.5), eyes.z - (pos.getZ() + 0.5));
        return buildAt(player, pos, face);
    }

    /** Builds at {@code pos}, as if a block face pointing {@code face} had been clicked there. */
    public static Result buildAt(ServerPlayer player, BlockPos pos, Direction face) {
        if (!Energy.canPowerSystems(player)) {
            return reply(player, Result.NO_POWER, "message.void_reclaimer.nanite.no_power");
        }
        Source source = source(player);
        if (source == null) {
            return reply(player, Result.NO_BLOCKS, "message.void_reclaimer.nanite.no_blocks");
        }
        BlockPlaceContext context = new BlockPlaceContext(player.level(), player, source.hand(), source.stack(),
                new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
        if (!context.getLevel().getBlockState(pos).canBeReplaced(context)) {
            return reply(player, Result.BLOCKED, "message.void_reclaimer.nanite.blocked");
        }
        InteractionResult placed = ((BlockItem) source.stack().getItem()).place(context);
        if (!placed.consumesAction()) {
            return reply(player, Result.BLOCKED, "message.void_reclaimer.nanite.blocked");
        }
        Energy.consume(player, Config.NANITE_ENERGY.get().floatValue());
        Replicator.countBlockPlaced(player);
        // BlockItem#place plays its sound for everyone but the placer (whose client normally predicts it).
        player.level().playSound(null, pos, ModSounds.NANITE_BUILD.get(), SoundSource.PLAYERS, 0.6f, 1.2f);
        return Result.BUILT;
    }

    private static Result reply(ServerPlayer player, Result result, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
        return result;
    }

    /** The stack to build from and the hand it counts as held in. */
    public record Source(ItemStack stack, InteractionHand hand) {}
}
