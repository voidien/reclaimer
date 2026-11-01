package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Trail;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.registry.OrderedRegistry;
import com.voidreclaimer.body.InventoryLock;
import com.voidreclaimer.client.screen.ModuleWheelScreen;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.flight.FlightPhysics;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.registry.ModAttachments;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import org.joml.Vector3f;

/** Client side of the body: the module wheel key, greyed-out locked inventory slots, and thruster trails. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientBodyController {
    private static final int LOCKED_OVERLAY = 0xC0101418;
    private static final float TRAIL_SIZE = 0.6f;

    private ClientBodyController() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        while (ModKeyMappings.MODULE_WHEEL.consumeClick()) {
            // Left Alt is Free Look at the Helm.
            if (mc.screen == null && com.voidreclaimer.body.Modules.hasToolMounts(player) && !ClientPilotController.isPiloting(player)) {
                mc.setScreen(new ModuleWheelScreen());
            }
        }
        if (!mc.isPaused()) {
            spawnTrail(player);
        }
    }

    /** Exhaust particles opposite the thrust direction while thrusting. */
    private static void spawnTrail(LocalPlayer player) {
        Cosmetics cosmetics = Bodies.data(player).cosmetics();
        FlightInput input = player.getData(ModAttachments.FLIGHT_INPUT);
        Trail trail = OrderedRegistry.get(player.registryAccess(), VoidReclaimerRegistries.TRAIL, cosmetics.trail());
        boolean thrusting = trail != null && trail.particles() && input.isThrusting() && ThrusterFlight.isActive(player)
                && com.voidreclaimer.client.render.body.ThrusterExhaust.installed(Bodies.data(player).modules().keySet());
        com.voidreclaimer.client.render.body.BoneProbe.want(thrusting);
        if (!thrusting) {
            return;
        }
        Vec3 thrust = FlightPhysics.thrustDirection(player.getXRot(), player.getYRot(), input);
        if (thrust.lengthSqr() < 1.0E-6) {
            return;
        }
        int rgb = trail.color(cosmetics.accent());
        Vector3f color = new Vector3f((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        Vec3 exhaust = thrust.normalize().scale(-1);
        double scale = player.getScale();
        Vec3 velocity = player.getDeltaMovement().add(exhaust.scale(0.15));
        // The mounts are only known while the body is drawn in the world: not in first person, and not behind a screen, which
        // draws it in GUI pixels.
        Minecraft mc = Minecraft.getInstance();
        List<Vec3> origins = mc.screen != null || mc.options.getCameraType().isFirstPerson() ? List.of()
                : com.voidreclaimer.client.render.body.ThrusterExhaust.origins(com.voidreclaimer.client.render.body.BoneProbe.latest(), mc.gameRenderer.getMainCamera().getPosition());
        if (origins.isEmpty()) {
            origins = List.of(player.getBoundingBox().getCenter().add(exhaust.scale(0.6 * scale)));
        }
        for (Vec3 origin : origins) {
            player.level().addParticle(new DustParticleOptions(color, TRAIL_SIZE * (float) Math.max(0.5, scale)),
                    origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }
    }

    @SubscribeEvent
    static void onContainerBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory && InventoryLock.isLocked(player, slot.getContainerSlot())) {
                int x = screen.getGuiLeft() + slot.x;
                int y = screen.getGuiTop() + slot.y;
                event.getGuiGraphics().fill(x, y, x + 16, y + 16, LOCKED_OVERLAY);
            }
        }
    }
}
