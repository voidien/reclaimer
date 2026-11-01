package com.voidreclaimer.mixin;

import net.neoforged.neoforge.internal.CommonModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a start-up deadlock between mods. Mods are constructed in parallel. Create, through Registrate's {@code addLayer},
 * starts initializing {@code RenderStateShard}, which needs its nested class {@code OutputStateShard}; Veil, in
 * {@code VeilRenderSystem}, starts initializing {@code OutputStateShard}, which needs its parent {@code RenderStateShard}. If
 * both start at once, each waits on the other for good and the game never opens. It hit about one start in six on CI.
 *
 * <p>Initializing the render classes on the main thread, before mods are constructed, leaves nothing to race for. Nothing in
 * them needs a GL context: the threads in the deadlock had none either.
 */
@Mixin(CommonModLoader.class)
abstract class RenderClassesFirstMixin {
    @Inject(method = "begin", at = @At("HEAD"))
    private static void void_reclaimer$renderClassesFirst(Runnable periodicTask, boolean datagen, CallbackInfo ci) {
        try {
            Class.forName("net.minecraft.client.renderer.RenderType", true, CommonModLoader.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("the render classes are missing from a client", e);
        }
    }
}
