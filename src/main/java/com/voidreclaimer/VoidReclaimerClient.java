package com.voidreclaimer;

import com.voidreclaimer.client.hud.DeathMarkerLayer;
import com.voidreclaimer.client.hud.DustMeterLayer;
import com.voidreclaimer.client.hud.DustSensorLayer;
import com.voidreclaimer.client.hud.EnergyBarLayer;
import com.voidreclaimer.client.hud.FlightHudLayer;
import com.voidreclaimer.client.hud.ModuleHudLayer;
import com.voidreclaimer.client.hud.TetherHudLayer;
import com.voidreclaimer.client.input.ClientReplicatorController;
import com.voidreclaimer.client.input.ClientSensorController;
import com.voidreclaimer.client.input.ModKeyMappings;
import com.voidreclaimer.client.render.DeepSpaceEffects;
import com.voidreclaimer.client.screen.BackupCoreScreen;
import com.voidreclaimer.core.BackupCoreBlock;
import com.voidreclaimer.network.SensorEchoPayload;
import com.voidreclaimer.replicator.ReplicatorData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = VoidReclaimer.MOD_ID, dist = Dist.CLIENT)
public final class VoidReclaimerClient {
    public VoidReclaimerClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Mods > Void Reclaimer > Config: lists the client and common settings.
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
        modBus.addListener(VoidReclaimerClient::onRegisterDimensionEffects);
        modBus.addListener(VoidReclaimerClient::onRegisterGuiLayers);
        modBus.addListener(ModKeyMappings::register);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(com.voidreclaimer.registry.ModMenus.BODY_INVENTORY.get(), com.voidreclaimer.client.screen.BodyInventoryScreen::new));
        modBus.addListener(VoidReclaimerClient::onRegisterClientExtensions);
        modBus.addListener(VoidReclaimerClient::onRegisterRenderers);
        modBus.addListener(VoidReclaimerClient::onRegisterItemColors);
        SensorEchoPayload.clientHandler = ClientSensorController::onEcho;
        com.voidreclaimer.network.HelmReadoutPayload.clientHandler = com.voidreclaimer.client.input.ClientPilotController::onReadout;
        com.voidreclaimer.network.LaunchStreakPayload.clientHandler = com.voidreclaimer.client.render.DeepSpaceEffects::onLaunchStreak;
        ReplicatorData.clientSyncListener = ClientReplicatorController::onSync;
        BackupCoreBlock.screenOpener = pos -> Minecraft.getInstance().setScreen(new BackupCoreScreen(pos));
    }

    /** Our fluids use vanilla water's textures, tinted. */
    private static void onRegisterClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for (var fluid : com.voidreclaimer.registry.ModFluids.ALL) {
            event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override
                public net.minecraft.resources.ResourceLocation getStillTexture() {
                    return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still");
                }

                @Override
                public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                    return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow");
                }

                @Override
                public int getTintColor() {
                    return fluid.tint();
                }
            }, fluid.type());
        }
    }

    private static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.voidreclaimer.registry.ModEntities.TEST_STARSHIP.get(), com.voidreclaimer.client.render.TestStarshipRenderer::new);
        event.registerEntityRenderer(com.voidreclaimer.registry.ModEntities.HELM_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(com.voidreclaimer.registry.ModEntities.HELPER_DRONE.get(), com.voidreclaimer.client.render.HelperDroneRenderer::new);
        event.registerEntityRenderer(com.voidreclaimer.registry.ModEntities.BODY_HOST.get(), com.voidreclaimer.client.render.body.BodyHostRenderer::new);
        event.registerBlockEntityRenderer(com.voidreclaimer.registry.ModBlockEntities.HELM.get(), com.voidreclaimer.client.render.HelmRenderer::new);
        event.registerBlockEntityRenderer(com.voidreclaimer.registry.ModBlockEntities.LAUNCH_CONTROLLER.get(), com.voidreclaimer.client.render.LaunchComplexRenderer::new);
    }

    /** The Test Starship item uses vanilla's spawn egg model: brass shell, cyan spots. */
    private static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0 ? 0xFFBF8B3A : 0xFF2FC0E0, com.voidreclaimer.registry.ModItems.TEST_STARSHIP.get());
    }

    private static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(DeepSpaceEffects.ID, new DeepSpaceEffects());
        event.register(com.voidreclaimer.client.render.RuinedEarthEffects.ID, new com.voidreclaimer.client.render.RuinedEarthEffects());
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, EnergyBarLayer::render);
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, com.voidreclaimer.client.hud.HealthBarLayer::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, VoidReclaimer.id("flight"), FlightHudLayer::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, VoidReclaimer.id("pilot"), com.voidreclaimer.client.hud.PilotHudLayer::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, VoidReclaimer.id("tether"), TetherHudLayer::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, VoidReclaimer.id("module"), ModuleHudLayer::render);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, VoidReclaimer.id("death_marker"), DeathMarkerLayer::render);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, VoidReclaimer.id("station_markers"), com.voidreclaimer.client.hud.StationMarkerLayer::render);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, VoidReclaimer.id("dust_meter"), DustMeterLayer::render);
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, VoidReclaimer.id("dust_sensor"), DustSensorLayer::render);
    }
}
