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
            net.minecraft.world.entity.player.Player player = net.minecraft.client.Minecraft.getInstance().player;
            float radius = player == null ? 0f : BodyBounds.radius(com.voidreclaimer.body.Bodies.view(player));
            event.setDistance(cameraDistance(event.getDistance(), event.getEntityScalingFactor(), radius) * com.voidreclaimer.client.camera.OrbitCamera.zoom());
        }

        @SubscribeEvent
        static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            event.setCanceled(true);
            BodyRenderer.get().render(event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    /** How many of the model's radii the third-person camera stands from a big body: it fills about the same share of the screen on all of them. */
    public static final float CAMERA_FIT = 3f;

    /**
     * Bodies much bigger than a player need the third-person camera further back to see them whole, by how big their model really is.
     * The event's distance is the unscaled 4 blocks and vanilla multiplies the result by the body's scale afterwards, so this works in
     * the model's own size: {@code radius} is {@link BodyBounds#radius}. The Ship ends up about 20 blocks away, the Shuttle about 30
     * and the Titan about 115. Bodies up to the Mech's size keep vanilla's distance.
     */
    public static float cameraDistance(float distance, float scale, float radius) {
        return scale <= 1.5f || radius <= 0f ? distance : CAMERA_FIT * radius;
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
