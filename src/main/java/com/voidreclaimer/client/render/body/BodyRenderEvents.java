package com.voidreclaimer.client.render.body;

import com.voidreclaimer.VoidReclaimer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Replaces the vanilla player model with the GeckoLib body, and keeps the painted textures in step with resource reloads. */
public final class BodyRenderEvents {
    private BodyRenderEvents() {}

    @EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
    public static final class Game {
        @SubscribeEvent
        static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
            event.setDistance(cameraDistance(event.getDistance(), event.getEntityScalingFactor()));
        }

        @SubscribeEvent
        static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            event.setCanceled(true);
            BodyRenderer.get().render(event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    /**
     * Bodies much bigger than a player (the Ship is 12 blocks long, the Titan over 60) need the third-person camera further back
     * to see them whole. The event's distance is the unscaled 4 blocks and vanilla multiplies the result by the body's scale
     * afterwards, so this only adds a factor of up to 4 for the big ones: the Ship ends up about 25 blocks away, the Titan
     * about 130 (a larger factor put the camera 1000 blocks away).
     */
    public static float cameraDistance(float distance, float scale) {
        return distance * Math.min(4f, Math.max(1f, scale * 0.7f));
    }

    @EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Mod {
        @SubscribeEvent
        static void onReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(BodyTextures.INSTANCE);
            event.registerReloadListener(BodyRigSpec.Loader.INSTANCE);
        }
    }
}
