package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoidReclaimer.MOD_ID);

    public static final Supplier<BlockEntityType<BackupCoreBlockEntity>> BACKUP_CORE = BLOCK_ENTITIES.register("backup_core",
            () -> BlockEntityType.Builder.of(BackupCoreBlockEntity::new, ModBlocks.BACKUP_CORE.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.ElectrolyzerBlockEntity>> ELECTROLYZER = BLOCK_ENTITIES.register("electrolyzer",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.ElectrolyzerBlockEntity::new, ModBlocks.ELECTROLYZER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.SpinnerBlockEntity>> SPINNER = BLOCK_ENTITIES.register("spinner",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.SpinnerBlockEntity::new, ModBlocks.SPINNER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.CloudSiphonBlockEntity>> CLOUD_SIPHON = BLOCK_ENTITIES.register("cloud_siphon",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.CloudSiphonBlockEntity::new, ModBlocks.CLOUD_SIPHON.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.MassDriverBaseBlockEntity>> MASS_DRIVER_BASE = BLOCK_ENTITIES.register("kinetic_drive_base",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.MassDriverBaseBlockEntity::new, ModBlocks.KINETIC_DRIVE_BASE.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.PayloadChamberBlockEntity>> PAYLOAD_CHAMBER = BLOCK_ENTITIES.register("payload_chamber",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.PayloadChamberBlockEntity::new, ModBlocks.PAYLOAD_CHAMBER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.launch.LaunchControllerBlockEntity>> LAUNCH_CONTROLLER = BLOCK_ENTITIES.register("launch_controller",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.launch.LaunchControllerBlockEntity::new, ModBlocks.LAUNCH_CONTROLLER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.QuantumReceiverBlockEntity>> QUANTUM_RECEIVER = BLOCK_ENTITIES.register("quantum_receiver",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.QuantumReceiverBlockEntity::new, ModBlocks.QUANTUM_RECEIVER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.terraform.SpireBlockEntity>> SPIRE = BLOCK_ENTITIES.register("terraforming_spire",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.terraform.SpireBlockEntity::new, ModBlocks.TERRAFORMING_SPIRE.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.ship.ChemicalThrusterBlockEntity>> CHEMICAL_THRUSTER = BLOCK_ENTITIES.register("chemical_thruster",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.ship.ChemicalThrusterBlockEntity::new, ModBlocks.CHEMICAL_THRUSTER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.ship.IonThrusterBlockEntity>> ION_THRUSTER = BLOCK_ENTITIES.register("ion_thruster",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.ship.IonThrusterBlockEntity::new, ModBlocks.ION_THRUSTER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.ship.RcsThrusterBlockEntity>> RCS_THRUSTER = BLOCK_ENTITIES.register("rcs_thruster",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.ship.RcsThrusterBlockEntity::new, ModBlocks.RCS_THRUSTER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.ship.HelmBlockEntity>> HELM = BLOCK_ENTITIES.register("helm",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.ship.HelmBlockEntity::new, ModBlocks.HELM.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.station.StationCoreBlockEntity>> STATION_CORE = BLOCK_ENTITIES.register("station_core",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.station.StationCoreBlockEntity::new, ModBlocks.STATION_CORE.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.station.DockingClampBlockEntity>> DOCKING_CLAMP = BLOCK_ENTITIES.register("docking_clamp",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.station.DockingClampBlockEntity::new, ModBlocks.DOCKING_CLAMP.get()).build(null));

    private ModBlockEntities() {}
}
