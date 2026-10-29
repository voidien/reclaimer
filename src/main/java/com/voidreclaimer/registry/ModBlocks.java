package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.asteroid.AsteroidBlock;
import com.voidreclaimer.core.BackupCoreBlock;
import com.voidreclaimer.dust.DustKinds;
import net.minecraft.resources.ResourceLocation;
import com.voidreclaimer.tether.TetherAnchorBlock;
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

    /** Leash point and recall target for the AI. */
    public static final DeferredBlock<TetherAnchorBlock> TETHER_ANCHOR = BLOCKS.registerBlock("tether_anchor", TetherAnchorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f).sound(SoundType.COPPER).lightLevel(state -> 4));

    /** Respawn point picked on the death screen. */
    public static final DeferredBlock<BackupCoreBlock> BACKUP_CORE = BLOCKS.registerBlock("backup_core", BackupCoreBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0f).sound(SoundType.COPPER).lightLevel(state -> 7));

    /** Every asteroid block: only the Mechanical Drill and the Matter Harvester break them. */
    public static final TagKey<Block> ASTEROID_BLOCKS = BlockTags.create(VoidReclaimer.id("asteroid_blocks"));

    public static final DeferredBlock<AsteroidBlock> ASTEROID_SILICATE = asteroid("asteroid_silicate", DustKinds.SILICATE, MapColor.STONE);
    public static final DeferredBlock<AsteroidBlock> ASTEROID_ICE = asteroid("asteroid_ice", DustKinds.ICE, MapColor.ICE);
    public static final DeferredBlock<AsteroidBlock> ASTEROID_CARBON = asteroid("asteroid_carbon", DustKinds.CARBON, MapColor.COLOR_BLACK);
    public static final DeferredBlock<AsteroidBlock> ASTEROID_METALLIC = asteroid("asteroid_metallic", DustKinds.METALLIC, MapColor.RAW_IRON);

    private static DeferredBlock<AsteroidBlock> asteroid(String name, ResourceLocation kind, MapColor color) {
        return BLOCKS.registerBlock(name, properties -> new AsteroidBlock(kind, properties),
                BlockBehaviour.Properties.of().mapColor(color).strength(3.0f, 12.0f).sound(SoundType.STONE));
    }

    /** The asteroid block made of a dust kind; silicate for any other kind. */
    public static AsteroidBlock asteroidBlock(ResourceLocation kind) {
        if (kind.equals(DustKinds.ICE)) {
            return ASTEROID_ICE.get();
        } else if (kind.equals(DustKinds.CARBON)) {
            return ASTEROID_CARBON.get();
        } else if (kind.equals(DustKinds.METALLIC)) {
            return ASTEROID_METALLIC.get();
        }
        return ASTEROID_SILICATE.get();
    }

    private ModBlocks() {}
}
