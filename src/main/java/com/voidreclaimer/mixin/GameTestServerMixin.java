package com.voidreclaimer.mixin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Test infrastructure only: vanilla's GameTest server bakes its world from an empty dimension registry, so datapack
 * dimensions such as {@code void_reclaimer:deep_space} don't exist there. This passes the loaded datapack dimensions
 * instead, so GameTests can run in deep space. It has no effect on normal clients or servers.
 */
@Mixin(GameTestServer.class)
abstract class GameTestServerMixin {
    @Redirect(
            method = "lambda$create$1",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static WorldDimensions.Complete void_reclaimer$bakeWithDatapackDimensions(
            WorldDimensions dimensions, Registry<LevelStem> emptyRegistry, LevelSettings settings, WorldLoader.DataLoadContext context) {
        return dimensions.bake(context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));
    }
}
