package com.voidreclaimer.link;

import com.mojang.serialization.MapCodec;
import com.voidreclaimer.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Uplink Seat: where a player leaves the main body to run a shell. See {@link UplinkSeatBlockEntity}. */
public class UplinkSeatBlock extends BaseEntityBlock {
    public static final MapCodec<UplinkSeatBlock> CODEC = simpleCodec(UplinkSeatBlock::new);

    public UplinkSeatBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<UplinkSeatBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UplinkSeatBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.UPLINK_SEAT.get(), UplinkSeatBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof UplinkSeatBlockEntity seat) {
            seat.showPicker(server);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
