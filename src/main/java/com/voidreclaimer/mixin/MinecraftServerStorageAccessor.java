package com.voidreclaimer.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets a link write {@code level.dat} at once: that file, not the player file, is what singleplayer loads the player from. */
@Mixin(MinecraftServer.class)
public interface MinecraftServerStorageAccessor {
    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess void_reclaimer$storageSource();
}
