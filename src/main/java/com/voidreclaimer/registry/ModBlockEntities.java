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

    private ModBlockEntities() {}
}
