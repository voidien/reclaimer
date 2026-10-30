package com.voidreclaimer.registry;

import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.mojang.serialization.MapCodec;
import com.voidreclaimer.api.body.Casing;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.ToolModuleType;
import com.voidreclaimer.api.body.Trail;
import com.voidreclaimer.api.dust.DustKind;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.milestone.MilestoneCriterion;
import com.voidreclaimer.body.ToolModules;
import com.voidreclaimer.replicator.MilestoneCriteria;
import net.minecraft.core.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** Creates the mod's own registries (keys in {@link VoidReclaimerRegistries}). */
public final class ModRegistries {
    public static final Registry<MapCodec<? extends MilestoneCriterion>> MILESTONE_CRITERION_TYPES =
            new RegistryBuilder<>(VoidReclaimerRegistries.MILESTONE_CRITERION_TYPE).create();

    public static final Registry<ToolModuleType> TOOL_MODULE_TYPES =
            new RegistryBuilder<>(VoidReclaimerRegistries.TOOL_MODULE_TYPE).sync(true).create();

    private ModRegistries() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModRegistries::onNewRegistries);
        modBus.addListener(ModRegistries::onNewDatapackRegistries);
        MilestoneCriteria.TYPES.register(modBus);
        ToolModules.TYPES.register(modBus);
    }

    private static void onNewRegistries(NewRegistryEvent event) {
        event.register(MILESTONE_CRITERION_TYPES);
        event.register(TOOL_MODULE_TYPES);
    }

    private static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // Synced: the client needs them for the HUD, cloud generation and screens.
        event.dataPackRegistry(VoidReclaimerRegistries.DUST_KIND, DustKind.CODEC, DustKind.CODEC);
        event.dataPackRegistry(VoidReclaimerRegistries.MILESTONE, Milestone.CODEC, Milestone.CODEC);
        event.dataPackRegistry(VoidReclaimerRegistries.REPLICATOR_CATEGORY, ReplicatorCategory.CODEC, ReplicatorCategory.CODEC);
        event.dataPackRegistry(VoidReclaimerRegistries.CASING, Casing.CODEC, Casing.CODEC);
        event.dataPackRegistry(VoidReclaimerRegistries.TRAIL, Trail.CODEC, Trail.CODEC);
        event.dataPackRegistry(VoidReclaimerRegistries.SIZE_CLASS, SizeClass.CODEC, SizeClass.CODEC);
        event.dataPackRegistry(VoidReclaimerRegistries.CHASSIS, Chassis.CODEC, Chassis.CODEC);
        // Server only: the pulse runs on the server, so this one is not synced.
        event.dataPackRegistry(VoidReclaimerRegistries.TERRAFORM_PROFILE, com.voidreclaimer.api.terraform.TerraformProfile.CODEC);
    }
}
