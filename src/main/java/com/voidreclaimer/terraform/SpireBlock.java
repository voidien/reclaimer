package com.voidreclaimer.terraform;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Terraforming Spire: the controller of a tower ({@link SpireAssembly}). It faces the tower, which stands in front of
 * it, and takes its shaft on the face behind it. Sneak-use assembles or sets down the tower; once assembled, a right-click
 * or a redstone pulse charges and fires the Genesis Pulse (ruined Earth only). Right-click with a catalyst or a fluid
 * container to load it. A Spire placed before towers existed loads as this block facing north, with no tower yet.
 */
public class SpireBlock extends HorizontalKineticBlock implements IBE<SpireBlockEntity> {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SpireBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    /** The tower stands in front of the player placing the Spire. */
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
    public Class<SpireBlockEntity> getBlockEntityClass() {
        return SpireBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SpireBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SPIRE.get();
    }

    /** A wrench must not turn the Spire away from a standing tower: the frame and column are found from its facing. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SpireBlockEntity spire && spire.assembled()) {
            return InteractionResult.PASS;
        }
        return super.onWrenched(state, context);
    }

    /** Right-click with a catalyst loads it; with a bucket or other container, fills a tank. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof SpireBlockEntity spire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModTags.SPIRE_CATALYSTS)) {
            if (!level.isClientSide) {
                ItemStack left = spire.items().insertItem(0, stack.copy(), false);
                if (!player.getAbilities().instabuild) {
                    stack.setCount(left.getCount());
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SpireBlockEntity spire)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (player.isShiftKeyDown()) {
            SpireAssembly.toggle(spire, player);
        } else {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.spire." + spire.tryStart().key()), true);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered && level.getBlockEntity(pos) instanceof SpireBlockEntity spire) {
                spire.tryStart();
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof SpireBlockEntity spire) {
            SpireAssembly.disassemble(spire);
            ItemStack catalysts = spire.catalyst().getStackInSlot(0);
            if (!catalysts.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, catalysts.copy()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
