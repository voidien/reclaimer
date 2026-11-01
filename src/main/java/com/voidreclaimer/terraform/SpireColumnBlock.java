package com.voidreclaimer.terraform;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The invisible, solid block that fills a Spire tower's column while it stands, so the drawn tower has a body. Placed and
 * removed by {@link SpireAssembly}; it drops nothing and has no item.
 */
public class SpireColumnBlock extends Block {
    public SpireColumnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
