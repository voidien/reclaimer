package com.voidreclaimer.machine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Hooks the machines into Create: stress values, the freezing fan type, spout casting, and the item and fluid handlers. */
public final class MachineSetup {
    public static final DeferredRegister<FanProcessingType> FAN_TYPES = DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, VoidReclaimer.MOD_ID);

    public static final DeferredHolder<FanProcessingType, FreezingFanType> FREEZING = FAN_TYPES.register("freezing", FreezingFanType::new);

    private MachineSetup() {}

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(ModBlocks.ELECTROLYZER.get(), () -> Config.ELECTROLYZER_STRESS.get());
            BlockStressValues.IMPACTS.register(ModBlocks.SPINNER.get(), () -> Config.SPINNER_STRESS.get());
            BlockSpoutingBehaviour.BY_BLOCK.register(AllBlocks.DEPOT.get(), new SpoutCasting());
        });
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, com.voidreclaimer.registry.ModBlockEntities.ELECTROLYZER.get(),
                (entity, side) -> entity.fluids());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, com.voidreclaimer.registry.ModBlockEntities.SPINNER.get(),
                (entity, side) -> entity.items());
    }
}
