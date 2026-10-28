package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoidReclaimer.MOD_ID);

    /** Blocks the Salvage Tool can dismantle for a dust refund: things the AI built, never natural rock. */
    public static final TagKey<Block> SALVAGEABLE = BlockTags.create(VoidReclaimer.id("salvageable"));

    /** Cheap, light silicate build block; the first thing the AI can build in the void. */
    public static final DeferredBlock<Block> SCAFFOLD = BLOCKS.registerSimpleBlock("scaffold",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.8f).sound(SoundType.COPPER_GRATE));

    private ModBlocks() {}
}
