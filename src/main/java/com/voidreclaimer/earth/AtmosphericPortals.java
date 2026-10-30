package com.voidreclaimer.earth;

import com.voidreclaimer.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The level-aware side of lighting an Atmospheric Portal: finding the frame and filling it. */
public final class AtmosphericPortals {
    private AtmosphericPortals() {}

    /** The closed frame rectangle that has {@code clicked} in its ring, found from any empty block next to it. */
    public static Optional<PortalShape.Rect> findFrame(Level level, BlockPos clicked) {
        for (Direction face : Direction.values()) {
            BlockPos start = clicked.relative(face);
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                Optional<PortalShape.Rect> rect = PortalShape.find(
                        pos -> level.getBlockState(pos).is(ModBlocks.PORTAL_FRAME.get()),
                        pos -> {
                            BlockState state = level.getBlockState(pos);
                            return state.isAir() || state.is(ModBlocks.ATMOSPHERIC_PORTAL.get());
                        },
                        start, axis);
                if (rect.isPresent()) {
                    return rect;
                }
            }
        }
        return Optional.empty();
    }

    /** Lights the frame that {@code clicked} belongs to. True when a portal was made. */
    public static boolean tryLight(Level level, BlockPos clicked) {
        Optional<PortalShape.Rect> rect = findFrame(level, clicked);
        if (rect.isEmpty()) {
            return false;
        }
        BlockState pane = ModBlocks.ATMOSPHERIC_PORTAL.get().defaultBlockState().setValue(AtmosphericPortalBlock.AXIS, rect.get().axis());
        rect.get().forEach(pos -> level.setBlock(pos, pane, Block.UPDATE_CLIENTS));
        return true;
    }
}
