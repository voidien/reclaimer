package com.voidreclaimer.core;

import com.mojang.serialization.MapCodec;
import com.voidreclaimer.nanite.NaniteSwarmItem;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlockEntities;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Backup Core: a respawn point the AI can pick on the death screen. Each player may own {@link BackupCoreList#MAX_CORES};
 * only the owner can open it (rename, charge with dust).
 */
public final class BackupCoreBlock extends BaseEntityBlock {
    public static final MapCodec<BackupCoreBlock> CODEC = simpleCodec(BackupCoreBlock::new);

    /** Client hook that opens the core screen; installed by the client entrypoint so common code stays client-free. */
    public static volatile Consumer<BlockPos> screenOpener = pos -> {};

    public BackupCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null && player.getData(ModAttachments.BACKUP_CORES).isFull()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("message.void_reclaimer.core.limit", BackupCoreList.MAX_CORES)
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof BackupCoreBlockEntity core) {
            BackupCores.register(player, core);
        }
    }

    /** Holding a block or the Nanite Swarm builds against this block instead of using it, like any other block. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem || stack.getItem() instanceof NaniteSwarmItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BackupCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (core.owner() != null && !core.isOwnedBy(player.getUUID())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.void_reclaimer.core.not_owner").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide()) {
            screenOpener.accept(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof BackupCoreBlockEntity core) {
            BackupCores.onCoreRemoved(serverLevel, pos, core.owner());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackupCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BACKUP_CORE.get(), BackupCoreBlockEntity::serverTick);
    }
}
