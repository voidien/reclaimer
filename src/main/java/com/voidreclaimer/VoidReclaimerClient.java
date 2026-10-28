package com.voidreclaimer;

import com.voidreclaimer.client.hud.EnergyBarLayer;
import com.voidreclaimer.client.hud.FlightHudLayer;
import com.voidreclaimer.client.input.ModKeyMappings;
import com.voidreclaimer.client.render.DeepSpaceEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = VoidReclaimer.MOD_ID, dist = Dist.CLIENT)
public final class VoidReclaimerClient {
    public VoidReclaimerClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(VoidReclaimerClient::onRegisterDimensionEffects);
        modBus.addListener(VoidReclaimerClient::onRegisterGuiLayers);
        modBus.addListener(ModKeyMappings::register);
    }

    private static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(DeepSpaceEffects.ID, new DeepSpaceEffects());
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, EnergyBarLayer::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, VoidReclaimer.id("flight"), FlightHudLayer::render);
    }
}
