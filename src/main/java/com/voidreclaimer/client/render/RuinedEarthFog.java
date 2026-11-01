package com.voidreclaimer.client.render;

import com.mojang.blaze3d.shaders.FogShape;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.earth.SkyLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Smog: terrain fog color and distance follow the sky on ruined Earth, so every restored sector thins it. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class RuinedEarthFog {
    private RuinedEarthFog() {}

    private static SkyLook look() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && RuinedEarth.is(level) ? SkyLook.in(level) : null;
    }

    @SubscribeEvent
    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        SkyLook look = look();
        if (look != null) {
            event.setRed(((look.fog() >> 16) & 0xFF) / 255f);
            event.setGreen(((look.fog() >> 8) & 0xFF) / 255f);
            event.setBlue((look.fog() & 0xFF) / 255f);
        }
    }

    @SubscribeEvent
    static void onFogDistance(ViewportEvent.RenderFog event) {
        SkyLook look = look();
        if (look == null || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        event.setNearPlaneDistance(0f);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), look.fogFar()));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
