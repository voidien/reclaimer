package com.voidreclaimer.terraform;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.terraform.TerraformProfile;
import com.voidreclaimer.registry.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/** The built-in terraform profiles. */
public final class TerraformProfiles {
    /** The profile every Spire uses. */
    public static final ResourceKey<TerraformProfile> RESTORED =
            ResourceKey.create(VoidReclaimerRegistries.TERRAFORM_PROFILE, VoidReclaimer.id("restored"));

    private TerraformProfiles() {}

    /** Ashen Slag becomes dirt under a grass skin, and the area becomes plains. */
    public static TerraformProfile restored() {
        return new TerraformProfile(List.of(new TerraformProfile.Swap(ModBlocks.ASHEN_SLAG.get(),
                Blocks.DIRT.defaultBlockState(), Optional.of(Blocks.GRASS_BLOCK.defaultBlockState()))), Biomes.PLAINS);
    }
}
