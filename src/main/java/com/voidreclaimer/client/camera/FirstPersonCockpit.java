package com.voidreclaimer.client.camera;

import com.voidreclaimer.ClientConfig;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.Bodies;
import net.minecraft.core.RegistryAccess;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person in a big ship (a chassis with a {@code cockpit}): no hand, and the camera sits at the cockpit instead of the player's
 * eyes ({@code CameraPhotoMixin} moves it). Drone and Mech keep the vanilla hand until their first-person pieces exist.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonCockpit {
    private FirstPersonCockpit() {}

    /** The cockpit of the player's chassis, or null (no cockpit, or switched off in the client config). */
    @Nullable
    public static Chassis.Cockpit of(Player player) {
        if (!ClientConfig.COCKPIT_CAMERA.get()) {
            return null;
        }
        // Asked every frame (camera and hand): rebuild the body view only when the body or the registries change.
        BodyData body = Bodies.data(player);
        RegistryAccess access = player.registryAccess();
        if (body != cachedBody || access != cachedAccess) {
            cachedBody = body;
            cachedAccess = access;
            cachedCockpit = Bodies.view(player).chassis().cockpit().orElse(null);
        }
        return cachedCockpit;
    }

    private static BodyData cachedBody;
    private static RegistryAccess cachedAccess;
    private static Chassis.Cockpit cachedCockpit;

    /** The hand is a vanilla arm and item: out of place in a ship, so it is hidden. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && of(player) != null) {
            event.setCanceled(true);
        }
    }
}
