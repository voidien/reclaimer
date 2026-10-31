package com.voidreclaimer.launch;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.voidreclaimer.machine.MassDriver;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Launch Controller: the one machine block of the launch complex. It faces the pad, which lies in front of it, and
 * takes its shaft on the face behind it. Right-click with a payload to load it by hand; sneak-use to assemble or set
 * down the complex ({@link LaunchComplex}).
 */
public class LaunchControllerBlock extends HorizontalKineticBlock implements IBE<LaunchControllerBlockEntity> {
    public LaunchControllerBlock(Properties properties) {
        super(properties);
    }

    /** The pad lies in front of the player placing the controller: they stand behind it and look across the pad. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Class<LaunchControllerBlockEntity> getBlockEntityClass() {
        return LaunchControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LaunchControllerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.LAUNCH_CONTROLLER.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!MassDriver.isPayload(stack) || player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller) {
            ItemStack left = stack.copy();
            for (int i = 0; i < LaunchControllerBlockEntity.SLOTS && !left.isEmpty(); i++) {
                left = controller.inventory().insertItem(i, left, false);
            }
            if (!player.getAbilities().instabuild) {
                stack.setCount(left.getCount());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            LaunchComplex.toggle(controller, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof LaunchControllerBlockEntity controller) {
            LaunchComplex.disassemble(controller);
            for (int i = 0; i < LaunchControllerBlockEntity.SLOTS; i++) {
                ItemStack stack = controller.inventory().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy()));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
