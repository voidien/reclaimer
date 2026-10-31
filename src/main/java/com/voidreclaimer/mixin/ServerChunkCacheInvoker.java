package com.voidreclaimer.mixin;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets {@link LevelPlotMixin} empty the chunk lookup's memory of the last chunks it found. */
@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheInvoker {
    @Invoker("clearCache")
    void void_reclaimer$clearCache();
}
