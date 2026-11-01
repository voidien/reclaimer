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

    /** The burnt, glassy crust that covers ruined Earth. Terraforming turns it back into soil. */
    public static final DeferredBlock<Block> ASHEN_SLAG = BLOCKS.registerSimpleBlock("ashen_slag",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.2f).sound(SoundType.BASALT));

    /** Frame of the Atmospheric Portal: build a flat rectangle of these and light it with a Portal Igniter. */
    public static final DeferredBlock<Block> PORTAL_FRAME = BLOCKS.registerSimpleBlock("portal_frame",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f, 8.0f).sound(SoundType.COPPER).requiresCorrectToolForDrops());

    /** The pane inside a lit frame. Not an item. */
    public static final DeferredBlock<com.voidreclaimer.earth.AtmosphericPortalBlock> ATMOSPHERIC_PORTAL = BLOCKS.registerBlock("atmospheric_portal",
            com.voidreclaimer.earth.AtmosphericPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollission().noOcclusion().strength(-1.0f)
                    .sound(SoundType.GLASS).lightLevel(state -> 11).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).noLootTable());

    /** Blocks that may form a Spire tower's frame. */
    public static final TagKey<Block> SPIRE_FRAME = BlockTags.create(VoidReclaimer.id("spire_frame"));
    /** The controller of a Spire tower: fires the Genesis Pulse that rewrites ruined ground around it. */
    public static final DeferredBlock<com.voidreclaimer.terraform.SpireBlock> TERRAFORMING_SPIRE = BLOCKS.registerBlock("terraforming_spire",
            com.voidreclaimer.terraform.SpireBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(3.0f, 8.0f).sound(SoundType.COPPER).lightLevel(state -> 8).noOcclusion());
    /** The invisible blocks that fill a Spire tower's column while it stands. */
    public static final DeferredBlock<com.voidreclaimer.terraform.SpireColumnBlock> SPIRE_COLUMN =
            BLOCKS.registerBlock("spire_column", com.voidreclaimer.terraform.SpireColumnBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion().noLootTable()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));

    /** Pushes a block ship: burns fuel, needs a shaft, redstone is the throttle. Its stress impact is in {@code MachineSetup}. */
    public static final DeferredBlock<com.voidreclaimer.ship.ChemicalThrusterBlock> CHEMICAL_THRUSTER = BLOCKS.registerBlock("chemical_thruster",
            com.voidreclaimer.ship.ChemicalThrusterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());

    /** Pushes a block ship on Forge Energy: weak but needs no fuel or shaft; redstone is the throttle. */
    public static final DeferredBlock<com.voidreclaimer.ship.IonThrusterBlock> ION_THRUSTER = BLOCKS.registerBlock("ion_thruster",
            com.voidreclaimer.ship.IonThrusterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0f, 6.0f).sound(SoundType.COPPER).lightLevel(state -> 3));

    /** The RCS Thruster placed: a small Forge Energy jet for fine rotation. Its item is a material, {@code ModMaterials.RCS_THRUSTER}. */
    public static final DeferredBlock<com.voidreclaimer.ship.RcsThrusterBlock> RCS_THRUSTER = BLOCKS.registerBlock("rcs_thruster",
            com.voidreclaimer.ship.RcsThrusterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f).sound(SoundType.COPPER));

    /** The pilot's chair: sit to fly the ship it is built into. GeckoLib model. */
    public static final DeferredBlock<com.voidreclaimer.ship.HelmBlock> HELM = BLOCKS.registerBlock("helm",
            com.voidreclaimer.ship.HelmBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN).strength(2.5f, 6.0f).sound(SoundType.COPPER).noOcclusion());

    /** The heart of a station: assembles its hull into a Sable ship, names it, holds a shared chest. */
    public static final DeferredBlock<com.voidreclaimer.station.StationCoreBlock> STATION_CORE = BLOCKS.registerBlock("station_core",
            com.voidreclaimer.station.StationCoreBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0f, 8.0f).sound(SoundType.COPPER).lightLevel(state -> 6));

    /** Holds a visiting ship against a station: docks what touches its face while powered. */
    public static final DeferredBlock<com.voidreclaimer.station.DockingClampBlock> DOCKING_CLAMP = BLOCKS.registerBlock("docking_clamp",
            com.voidreclaimer.station.DockingClampBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 8.0f).sound(SoundType.COPPER));

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

    /** SU-driven machines of the nebula chains. Their stress impact is registered in {@code MachineSetup}. */
    public static final DeferredBlock<com.voidreclaimer.machine.MachineBlock<com.voidreclaimer.machine.ElectrolyzerBlockEntity>> ELECTROLYZER =
            BLOCKS.registerBlock("electrolyzer", properties -> new com.voidreclaimer.machine.MachineBlock<>(properties,
                    com.voidreclaimer.machine.ElectrolyzerBlockEntity.class, ModBlockEntities.ELECTROLYZER::get),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<com.voidreclaimer.machine.MachineBlock<com.voidreclaimer.machine.SpinnerBlockEntity>> SPINNER =
            BLOCKS.registerBlock("spinner", properties -> new com.voidreclaimer.machine.MachineBlock<>(properties,
                    com.voidreclaimer.machine.SpinnerBlockEntity.class, ModBlockEntities.SPINNER::get),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());

    /** Sits in a dense dust cloud and condenses it into raw items; turned by a shaft. */
    public static final DeferredBlock<com.voidreclaimer.machine.MachineBlock<com.voidreclaimer.machine.CloudSiphonBlockEntity>> CLOUD_SIPHON =
            BLOCKS.registerBlock("cloud_siphon", properties -> new com.voidreclaimer.machine.MachineBlock<>(properties,
                    com.voidreclaimer.machine.CloudSiphonBlockEntity.class, ModBlockEntities.CLOUD_SIPHON::get),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());

    /** The Kinetic Mass Driver: the drive base (shaft input, controller), the payload chamber and the barrel nozzle, stacked in that order. */
    public static final DeferredBlock<com.voidreclaimer.machine.MachineBlock<com.voidreclaimer.machine.MassDriverBaseBlockEntity>> KINETIC_DRIVE_BASE =
            BLOCKS.registerBlock("kinetic_drive_base", properties -> new com.voidreclaimer.machine.MachineBlock<>(properties,
                    com.voidreclaimer.machine.MassDriverBaseBlockEntity.class, ModBlockEntities.MASS_DRIVER_BASE::get),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<com.voidreclaimer.machine.PayloadChamberBlock> PAYLOAD_CHAMBER =
            BLOCKS.registerBlock("payload_chamber", com.voidreclaimer.machine.PayloadChamberBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER));
    public static final DeferredBlock<Block> MAGNETIC_BARREL_NOZZLE =
            BLOCKS.registerSimpleBlock("magnetic_barrel_nozzle",
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER));
    /** Blocks that may form the launch complex's ring. */
    public static final TagKey<Block> LAUNCH_FRAME = BlockTags.create(VoidReclaimer.id("launch_frame"));
    /** The launch complex: the controller in the middle of the pad's near edge, and the invisible blocks that fill the pad while it is assembled. */
    public static final DeferredBlock<com.voidreclaimer.launch.LaunchControllerBlock> LAUNCH_CONTROLLER =
            BLOCKS.registerBlock("launch_controller", com.voidreclaimer.launch.LaunchControllerBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<com.voidreclaimer.launch.LaunchPadBlock> LAUNCH_PAD =
            BLOCKS.registerBlock("launch_pad", com.voidreclaimer.launch.LaunchPadBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion().noLootTable()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    /** The Sub-Light Quantum Receiver: a rotation source fed by the Dyson Sphere. */
    public static final DeferredBlock<com.voidreclaimer.machine.MachineBlock<com.voidreclaimer.machine.QuantumReceiverBlockEntity>> QUANTUM_RECEIVER =
            BLOCKS.registerBlock("quantum_receiver", properties -> new com.voidreclaimer.machine.MachineBlock<>(properties,
                    com.voidreclaimer.machine.QuantumReceiverBlockEntity.class, ModBlockEntities.QUANTUM_RECEIVER::get),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f, 6.0f).sound(SoundType.COPPER).noOcclusion());

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
