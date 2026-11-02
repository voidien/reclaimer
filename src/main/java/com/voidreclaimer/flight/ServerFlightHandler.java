package com.voidreclaimer.flight;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.tether.Tether;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
    /** The fastest the wind can carry a body, in blocks a second, for anything that goes by speed. */
    public static final double MOST_WIND = 30.0;
    private static final int OVERLAP_CHECK_TICKS = 10;
    /** Deeper than rounding or a push being worked out, in blocks. */
    private static final double DEEP_OVERLAP = 0.5;
    private static final int DEEP_OVERLAP_TICKS = 40;

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

        if (!player.getData(ModAttachments.BODY_HEARD).lagging()) {
            tracker.settle(new com.voidreclaimer.body.BodyHeading(player.getYRot(), player.getXRot()));
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
        if (Tether.isRecalling(player)) {
            // The server itself started the winch, which is faster than top speed: restart the window after it.
            tracker.clear();
        } else if (!Energy.isExempt(player) && (checkSpeed(player, tracker, position) || checkOverlap(player, tracker, position))) {
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

    /**
     * A dimension change leaves the client with no attachments, so its copy of the keys starts empty, and it only reports a
     * change. Keys let go during the load would never be reported, and the server would go on charging for them. Keys still
     * held differ from the client's empty copy, so they are sent again.
     */
    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().setData(ModAttachments.FLIGHT_INPUT, FlightInput.NONE);
    }

    private static void chargeEnergy(ServerPlayer player, double speed) {
        if (Energy.isExempt(player) || !Energy.canPowerSystems(player)) {
            return;
        }
        FlightInput input = player.getData(ModAttachments.FLIGHT_INPUT);
        double thrustPerTick = Config.THRUST_ENERGY_COST.get() / FlightStats.TICKS_PER_SECOND;
        double perTick;
        if (input.isThrusting()) {
            perTick = thrustPerTick;
            com.voidreclaimer.body.Wear.onThrustTick(player);
        } else {
            // Holding altitude under gravity spends the share of thrust that cancels weight, on top of any braking.
            double hover = hoverShare(player);
            perTick = hover * thrustPerTick;
            if (hover > 0.0) {
                com.voidreclaimer.body.Wear.onThrustTick(player);
            }
            if (ThrusterFlight.isBraking(player, input, speed)) {
                perTick += Config.BRAKE_ENERGY_COST.get() / FlightStats.TICKS_PER_SECOND;
            }
        }
        if (perTick <= 0.0) {
            return;
        }
        Energy.consume(player, (float) perTick * com.voidreclaimer.body.Bodies.stats(player).energyUse());
    }

    /** The share of thrust this player is spending to hold up its weight right now (0 in zero-G). */
    public static double hoverShare(Player player) {
        return FlightPhysics.hoverShare(FlightStats.of(player), ThrusterFlight.weight(player),
                player.getData(ModAttachments.FLIGHT_SETTINGS).dampener(), Energy.canPowerSystems(player));
    }

    /**
     * A body under power with the dampener on lands under control, however far it came down: vanilla's distance-based
     * damage would hurt every normal hover descent. A free fall (no power, or the dampener off) still hurts as in vanilla.
     */
    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !fallHurts(player)) {
            event.setCanceled(true);
        }
    }

    /** Whether vanilla's fall damage reaches this body. */
    public static boolean fallHurts(Player player) {
        return !(ThrusterFlight.isActive(player) && !Energy.isExempt(player) && Energy.canPowerSystems(player)
                && player.getData(ModAttachments.FLIGHT_SETTINGS).dampener())
                && !com.voidreclaimer.space.ZeroGravity.isZeroG(player.level(), player.blockPosition());
    }

    /**
     * A big body's boxes are flown on the client, so the server looks now and then. A body held deep in a block for two seconds goes back
     * to where it was last clear; a hair of overlap, or a body that has never been clear here, is left alone. Ships don't count: one can
     * move into a body by itself, and the client can't get out of that. Returns true if the player was corrected.
     */
    private static boolean checkOverlap(ServerPlayer player, FlightTracker tracker, Vec3 position) {
        if (player.tickCount % OVERLAP_CHECK_TICKS != 0) {
            return false;
        }
        var chassis = com.voidreclaimer.body.Bodies.view(player).chassis();
        if (chassis.sockets().stream().allMatch(socket -> socket.boxes().isEmpty())) {
            return false;
        }
        if (com.voidreclaimer.body.BoxCollision.overlap(player) < DEEP_OVERLAP) {
            tracker.clearOfBlocks(position, chassis);
            return false;
        }
        Vec3 clear = tracker.lastClear(chassis);
        if (clear == null || tracker.deepFor(OVERLAP_CHECK_TICKS) < DEEP_OVERLAP_TICKS) {
            return false;
        }
        VoidReclaimer.LOGGER.warn("{} stayed inside something for {} ticks; correcting", player.getName().getString(), DEEP_OVERLAP_TICKS);
        tracker.clear();
        pullBack(player, clear);
        return true;
    }

    private static void pullBack(ServerPlayer player, Vec3 to) {
        player.connection.teleport(to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    /** Returns true if the player was corrected. */
    private static boolean checkSpeed(ServerPlayer player, FlightTracker tracker, Vec3 position) {
        int ticks = tracker.size();
        if (ticks < WINDOW_TICKS) {
            return false;
        }
        // The oldest entry was recorded `ticks` ticks before the current position.
        Vec3 start = tracker.oldest();
        // The wind over ruined Earth can carry the body faster than its own top speed.
        double maxSpeed = FlightStats.of(player).maxSpeed()
                + (player.level().dimension() == com.voidreclaimer.earth.RuinedEarth.DIMENSION ? Config.EARTH_WIND_SPEED.get() / 20.0 : 0.0)
                // A turn pushes a body off what it swings into, on top of its own speed.
                + (player.getData(ModAttachments.BODY_HEARD).lagging() ? com.voidreclaimer.body.BodyBoxes.MOST_PUSH : 0.0);
        double tolerance = Config.SPEED_CHECK_TOLERANCE.get();
        if (!FlightPhysics.exceedsSpeedLimit(start, position, ticks, maxSpeed, tolerance, SLACK_BLOCKS)) {
            return false;
        }
        double moved = start.distanceTo(position);
        double allowed = maxSpeed * ticks * tolerance + SLACK_BLOCKS;
        VoidReclaimer.LOGGER.warn("{} moved {} blocks in {} ticks under thruster flight (allowed {}); correcting",
                player.getName().getString(), String.format("%.2f", moved), ticks, String.format("%.2f", allowed));
        tracker.clear();
        pullBack(player, start);
        return true;
    }
}
