package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.dust.DustKinds;
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
                    context.register(ResourceKey.create(VoidReclaimerRegistries.DUST_KIND, id), kind)));

    public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(VoidReclaimer.MOD_ID));
    }
}
