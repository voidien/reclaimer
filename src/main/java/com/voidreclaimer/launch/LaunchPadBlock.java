package com.voidreclaimer.launch;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The invisible, solid block that fills a launch complex's pad while it is assembled, so the drawn pad can be walked on.
 * Placed and removed by {@link LaunchComplex}; it drops nothing and has no item.
 */
public class LaunchPadBlock extends Block {
    public LaunchPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
