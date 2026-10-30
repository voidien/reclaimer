package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.replicator.ReplicatorCategories;
import com.voidreclaimer.replicator.Milestones;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

/** Writes the built-in entries of the mod's datapack registries. */
public final class ModDatapackProvider extends DatapackBuiltinEntriesProvider {
    static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(VoidReclaimerRegistries.DUST_KIND, context -> DustKinds.BUILTIN.forEach((id, kind) ->
                    context.register(ResourceKey.create(VoidReclaimerRegistries.DUST_KIND, id), kind)))
            .add(VoidReclaimerRegistries.MILESTONE, context -> Milestones.BUILTIN.forEach(context::register))
            .add(VoidReclaimerRegistries.REPLICATOR_CATEGORY, context -> ReplicatorCategories.BUILTIN.forEach(context::register))
            .add(VoidReclaimerRegistries.CASING, context -> Cosmetics.BUILTIN_CASINGS.forEach(context::register))
            .add(VoidReclaimerRegistries.TRAIL, context -> Cosmetics.BUILTIN_TRAILS.forEach(context::register))
            .add(VoidReclaimerRegistries.SIZE_CLASS, context -> BuiltinBodies.SIZE_CLASSES.forEach(context::register))
            .add(VoidReclaimerRegistries.CHASSIS, context -> BuiltinBodies.CHASSIS.forEach(context::register))
            .add(VoidReclaimerRegistries.TERRAFORM_PROFILE, context -> context.register(
                    com.voidreclaimer.terraform.TerraformProfiles.RESTORED, com.voidreclaimer.terraform.TerraformProfiles.restored()));

    public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(VoidReclaimer.MOD_ID));
    }
}
