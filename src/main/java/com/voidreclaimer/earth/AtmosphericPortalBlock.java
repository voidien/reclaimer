package com.voidreclaimer.earth;

import com.voidreclaimer.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shimmering pane inside a lit Atmospheric Portal frame. Thin, no collision, and it vanishes as soon as a block of
 * the frame around it is gone (like the Nether portal). Travel through it comes in a later change.
 */
public class AtmosphericPortalBlock extends Block implements Portal {
    /** The axis the portal's width runs along. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final int TRAVEL_TICKS = 5;
    private static final VoxelShape ACROSS_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ACROSS_Z = Block.box(6, 0, 0, 10, 16, 16);

    public AtmosphericPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /** Panes register themselves with the level, so ships can find the portals they might fly through. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            level.setData(com.voidreclaimer.registry.ModAttachments.PORTAL_PANES, level.getData(com.voidreclaimer.registry.ModAttachments.PORTAL_PANES).with(pos));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !newState.is(this)) {
            level.setData(com.voidreclaimer.registry.ModAttachments.PORTAL_PANES, level.getData(com.voidreclaimer.registry.ModAttachments.PORTAL_PANES).without(pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? ACROSS_X : ACROSS_Z;
    }

    /** Gone when a neighbor in the portal's plane is neither frame nor another pane of the same portal. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        Direction.Axis width = state.getValue(AXIS);
        boolean inPlane = direction.getAxis() == Direction.Axis.Y || direction.getAxis() == width;
        if (inPlane && !neighborState.is(ModBlocks.PORTAL_FRAME.get())
                && !(neighborState.is(this) && neighborState.getValue(AXIS) == width)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (PortalTravel.staysInOrbit(level, entity)) {
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.void_reclaimer.portal.stays_in_orbit"), true);
            }
        } else if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    /** Ticks inside the pane before the trip; short, because a ship passes through quickly. */
    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return TRAVEL_TICKS;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return PortalTravel.destination(level, entity, pos);
    }

    /** No screen overlay: the pane is not a Nether portal. */
    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.NONE;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) && adjacent.getValue(AXIS) == state.getValue(AXIS);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
