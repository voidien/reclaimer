package com.voidreclaimer.compat.create;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateRegistries;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlockEntities;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** What makes our blocks visible to Create's own logistics: for now, the Holo Projector as a Display Link target. */
public final class HoloDisplaySetup {
    public static final DeferredRegister<DisplayTarget> TARGETS = DeferredRegister.create(CreateRegistries.DISPLAY_TARGET, VoidReclaimer.MOD_ID);

    public static final DeferredHolder<DisplayTarget, HoloDisplayTarget> HOLO_PROJECTOR = TARGETS.register("holo_projector", HoloDisplayTarget::new);

    private HoloDisplaySetup() {}

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DisplayTarget.BY_BLOCK_ENTITY.register(ModBlockEntities.HOLO_PROJECTOR.get(), HOLO_PROJECTOR.get()));
    }
}
