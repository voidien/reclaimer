package com.voidreclaimer.client.render.body;

import com.voidreclaimer.VoidReclaimer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Replaces the vanilla player model with the GeckoLib body, and keeps the painted textures in step with resource reloads. */
public final class BodyRenderEvents {
    private BodyRenderEvents() {}

    @EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
    public static final class Game {
        @SubscribeEvent
        static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            event.setCanceled(true);
            BodyRenderer.get().render(event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    @EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Mod {
        @SubscribeEvent
        static void onReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(BodyTextures.INSTANCE);
        }
    }
}
