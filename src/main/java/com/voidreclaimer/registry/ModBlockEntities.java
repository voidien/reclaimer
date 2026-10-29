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

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.MassDriverBaseBlockEntity>> MASS_DRIVER_BASE = BLOCK_ENTITIES.register("kinetic_drive_base",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.MassDriverBaseBlockEntity::new, ModBlocks.KINETIC_DRIVE_BASE.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.PayloadChamberBlockEntity>> PAYLOAD_CHAMBER = BLOCK_ENTITIES.register("payload_chamber",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.PayloadChamberBlockEntity::new, ModBlocks.PAYLOAD_CHAMBER.get()).build(null));

    public static final Supplier<BlockEntityType<com.voidreclaimer.machine.QuantumReceiverBlockEntity>> QUANTUM_RECEIVER = BLOCK_ENTITIES.register("quantum_receiver",
            () -> BlockEntityType.Builder.of(com.voidreclaimer.machine.QuantumReceiverBlockEntity::new, ModBlocks.QUANTUM_RECEIVER.get()).build(null));

    private ModBlockEntities() {}
}
