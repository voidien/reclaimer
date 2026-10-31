package com.voidreclaimer.mixin;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the server's chunk lookup when a Sable plot changes its chunks. {@code ServerChunkCache.getChunk} remembers the last
 * four chunks it found and only forgets them when the chunk map changes in a way it notices, which a plot's chunks do not do.
 * Until then the lookup answers with what it remembers: a removed ship's dead chunk, or Sable's shared empty chunk for a plot
 * chunk that did not exist yet when someone looked. {@code SubLevelAssemblyHelper.moveBlocks} writes a block into the real
 * chunk and then asks the level for its block entity, so with a wrong answer the block entity's data went to the dead chunk or
 * nowhere, after the old block entity had been emptied: the new ship kept its blocks and lost their contents.
 */
@Mixin(LevelPlot.class)
abstract class LevelPlotMixin {
    @Shadow
    @Final
    protected SubLevelContainer container;

    @Inject(method = "addChunkHolder", at = @At("TAIL"))
    private void void_reclaimer$chunkAdded(ChunkPos local, PlotChunkHolder holder, boolean initializeLighting, CallbackInfo ci) {
        void_reclaimer$forgetLookups();
    }

    /** At the very end: while the plot's chunks are being removed, a block entity saying goodbye can still look them up. */
    @Inject(method = "onRemove", at = @At("TAIL"))
    private void void_reclaimer$chunksRemoved(CallbackInfo ci) {
        void_reclaimer$forgetLookups();
    }

    @Unique
    private void void_reclaimer$forgetLookups() {
        if (container.getLevel().getChunkSource() instanceof ServerChunkCache cache) {
            ((ServerChunkCacheInvoker) cache).void_reclaimer$clearCache();
        }
    }
}
