package com.voidreclaimer.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.item.ItemEntity;

/** The middle block of the Mass Driver. Right-click with a payload to load it by hand. */
public class PayloadChamberBlock extends Block implements EntityBlock {
    public PayloadChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PayloadChamberBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!MassDriver.isPayload(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PayloadChamberBlockEntity chamber) {
            ItemStack left = stack.copy();
            for (int i = 0; i < PayloadChamberBlockEntity.SLOTS && !left.isEmpty(); i++) {
                left = chamber.inventory().insertItem(i, left, false);
            }
            if (!player.getAbilities().instabuild) {
                stack.setCount(left.getCount());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof PayloadChamberBlockEntity chamber) {
            for (int i = 0; i < PayloadChamberBlockEntity.SLOTS; i++) {
                ItemStack stack = chamber.inventory().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy()));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
