package com.voidreclaimer.client.sound;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Starts the thruster and dampener loops when they become audible; each loop fades itself out and stops. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class FlightSounds {
    private static final double BRAKING_SPEED = 0.01;

    private static ThrusterSoundInstance thruster;
    private static ThrusterSoundInstance dampener;

    private FlightSounds() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            thruster = null;
            dampener = null;
            return;
        }
        if (isThrusting(player) && (thruster == null || thruster.isStopped())) {
            thruster = new ThrusterSoundInstance(player, ModSounds.THRUSTER.get(), () -> isThrusting(player), 0.35f, 1.0f);
            mc.getSoundManager().play(thruster);
        }
        if (isBraking(player) && (dampener == null || dampener.isStopped())) {
            dampener = new ThrusterSoundInstance(player, ModSounds.DAMPENER.get(), () -> isBraking(player), 0.25f, 1.0f);
            mc.getSoundManager().play(dampener);
        }
    }

    private static boolean poweredFlight(LocalPlayer player) {
        return !player.isRemoved() && ThrusterFlight.isActive(player) && Energy.canPowerSystems(player);
    }

    private static boolean isThrusting(LocalPlayer player) {
        return poweredFlight(player) && player.getData(ModAttachments.FLIGHT_INPUT).isThrusting();
    }

    private static boolean isBraking(LocalPlayer player) {
        return poweredFlight(player)
                && ThrusterFlight.isBraking(player, player.getData(ModAttachments.FLIGHT_INPUT), player.getDeltaMovement().length())
                && player.getDeltaMovement().length() > BRAKING_SPEED;
    }
}
