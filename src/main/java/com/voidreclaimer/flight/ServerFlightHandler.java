package com.voidreclaimer.flight;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side of thruster flight: charges energy for thrust and braking, and checks that the client-simulated
 * movement stays within the body's top speed.
 *
 * <p>The speed check looks at displacement over a one-second window rather than single ticks, so bunched-up move
 * packets don't cause false positives. A single-tick jump larger than any legal move is treated as a teleport and
 * restarts the window. On a violation the player is moved back to the start of the window and stopped.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ServerFlightHandler {
    private static final int WINDOW_TICKS = 20;
    /** Larger than anything thrusters allow per tick, smaller than vanilla's own "moved too quickly" limit. */
    private static final double TELEPORT_JUMP = 8.0;
    /** Slack for collision push-out and rounding. */
    private static final double SLACK_BLOCKS = 1.0;

    private ServerFlightHandler() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FlightTracker tracker = player.getData(ModAttachments.FLIGHT_TRACKER);
        if (!ThrusterFlight.isActive(player)) {
            tracker.clear();
            return;
        }

        Vec3 position = player.position();
        Vec3 previous = tracker.latest();
        double stepDistance = previous == null ? 0.0 : previous.distanceTo(position);
        if (stepDistance > TELEPORT_JUMP) {
            tracker.clear();
            stepDistance = 0.0;
        }

        tracker.setLastSpeed(stepDistance);
        chargeEnergy(player, stepDistance);
        if (!Energy.isExempt(player) && checkSpeed(player, tracker, position)) {
            return;
        }
        tracker.record(position, WINDOW_TICKS);
    }

    /** Commands, ender pearls, chorus fruit etc. move the player legitimately. */
    @SubscribeEvent
    static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ModAttachments.FLIGHT_TRACKER).clear();
        }
    }

    private static void chargeEnergy(ServerPlayer player, double speed) {
        if (Energy.isExempt(player) || !Energy.canPowerSystems(player)) {
            return;
        }
        FlightInput input = player.getData(ModAttachments.FLIGHT_INPUT);
        double perTick;
        if (input.isThrusting()) {
            perTick = Config.THRUST_ENERGY_COST.get() / FlightStats.TICKS_PER_SECOND;
        } else if (ThrusterFlight.isBraking(player, input, speed)) {
            perTick = Config.BRAKE_ENERGY_COST.get() / FlightStats.TICKS_PER_SECOND;
        } else {
            return;
        }
        Energy.consume(player, (float) perTick);
    }

    /** Returns true if the player was corrected. */
    private static boolean checkSpeed(ServerPlayer player, FlightTracker tracker, Vec3 position) {
        int ticks = tracker.size();
        if (ticks < WINDOW_TICKS) {
            return false;
        }
        // The oldest entry was recorded `ticks` ticks before the current position.
        Vec3 start = tracker.oldest();
        double maxSpeed = FlightStats.of(player).maxSpeed();
        double tolerance = Config.SPEED_CHECK_TOLERANCE.get();
        if (!FlightPhysics.exceedsSpeedLimit(start, position, ticks, maxSpeed, tolerance, SLACK_BLOCKS)) {
            return false;
        }
        double moved = start.distanceTo(position);
        double allowed = maxSpeed * ticks * tolerance + SLACK_BLOCKS;
        VoidReclaimer.LOGGER.warn("{} moved {} blocks in {} ticks under thruster flight (allowed {}); correcting",
                player.getName().getString(), String.format("%.2f", moved), ticks, String.format("%.2f", allowed));
        tracker.clear();
        player.connection.teleport(start.x, start.y, start.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        return true;
    }
}
