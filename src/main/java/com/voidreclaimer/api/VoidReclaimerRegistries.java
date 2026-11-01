package com.voidreclaimer.api;

import com.voidreclaimer.VoidReclaimer;
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
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Keys of every Void Reclaimer registry. Datapack registries are loaded from
 * {@code data/<namespace>/void_reclaimer/<registry>/<name>.json}; code registries are filled with
 * {@code DeferredRegister.create(key, modId)}.
 */
public final class VoidReclaimerRegistries {
    /** Synced datapack registry: what dust clouds are made of. */
    public static final ResourceKey<Registry<DustKind>> DUST_KIND = key("dust_kind");
    /** Synced datapack registry: progression milestones that unlock Replicator recipes. */
    public static final ResourceKey<Registry<Milestone>> MILESTONE = key("milestone");
    /** Synced datapack registry: Replicator screen tabs. */
    public static final ResourceKey<Registry<ReplicatorCategory>> REPLICATOR_CATEGORY = key("replicator_category");
    /** Synced datapack registry: body casing materials. */
    public static final ResourceKey<Registry<Casing>> CASING = key("casing");
    /** Synced datapack registry: thruster trail styles. */
    public static final ResourceKey<Registry<Trail>> TRAIL = key("trail");
    /** Synced datapack registry: body size classes (scale and rank). */
    public static final ResourceKey<Registry<SizeClass>> SIZE_CLASS = key("size_class");
    /** Synced datapack registry: body layouts (sockets) named by core sections. */
    public static final ResourceKey<Registry<Chassis>> CHASSIS = key("chassis");
    /** Datapack registry (server only): what a Terraforming Spire pulse turns ruined ground into. */
    public static final ResourceKey<Registry<com.voidreclaimer.api.terraform.TerraformProfile>> TERRAFORM_PROFILE = key("terraform_profile");
    /** Code registry: what tool modules do. */
    public static final ResourceKey<Registry<ToolModuleType>> TOOL_MODULE_TYPE = key("tool_module_type");
    /** Code registry: what a Holo Projector can show as a readout. */
    public static final ResourceKey<Registry<com.voidreclaimer.api.holo.HoloReadout<?>>> HOLO_READOUT = key("holo_readout");
    /** Code registry: the codecs of milestone criterion types, by the {@code type} name used in JSON. */
    public static final ResourceKey<Registry<MapCodec<? extends MilestoneCriterion>>> MILESTONE_CRITERION_TYPE =
            key("milestone_criterion_type");

    private VoidReclaimerRegistries() {}

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(VoidReclaimer.id(name));
    }
}
