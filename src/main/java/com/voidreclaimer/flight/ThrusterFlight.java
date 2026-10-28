package com.voidreclaimer.flight;

import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ZeroGravity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Zero-G thruster flight shared by both sides.
 *
 * <p>Thrust is relative to the view: forward/back along the look vector, strafe along the view's right vector
 * (always horizontal, since there is no roll), up/down along the view's up vector. With the inertial dampener on,
 * the body brakes to a stop whenever no thrust key is held. There is no drag otherwise, so with the dampener off
 * the body drifts until it thrusts the other way. Speed is hard-capped by {@link FlightStats#maxSpeed()}. In low
 * power mode thrusters and dampener are offline and the body drifts.
 *
 * <p>The local client simulates the movement (via {@code LivingEntityTravelMixin}); the server checks the result
 * in {@link ServerFlightHandler}.
 */
public final class ThrusterFlight {
    private ThrusterFlight() {}

    /** Whether the player moves by thrusters right now instead of vanilla movement. */
    public static boolean isActive(Player player) {
        if (player.isSpectator() || player.isPassenger() || player.isFallFlying()) {
            return false;
        }
        if (!ZeroGravity.isZeroG(player.level(), player.blockPosition())) {
            return false;
        }
        return !player.isCreative() || player.getData(ModAttachments.FLIGHT_SETTINGS).creativeThrusters();
    }

    /** Replaces vanilla travel for the local player: apply thrust, then move with collisions and no drag. */
    public static void travel(Player player) {
        Vec3 velocity = step(player, player.getDeltaMovement(), player.getData(ModAttachments.FLIGHT_INPUT));
        player.setDeltaMovement(velocity);
        player.move(MoverType.SELF, velocity);
        player.calculateEntityAnimation(true);
    }

    /** One tick of thruster physics for this player: returns the new velocity. */
    public static Vec3 step(Player player, Vec3 velocity, FlightInput input) {
        return FlightPhysics.step(
                velocity,
                FlightPhysics.thrustDirection(player.getXRot(), player.getYRot(), input),
                Energy.canPowerSystems(player),
                player.getData(ModAttachments.FLIGHT_SETTINGS).dampener(),
                FlightStats.of(player));
    }

    /** Whether the dampener is braking this tick (for energy cost). */
    public static boolean isBraking(Player player, FlightInput input, double speed) {
        return !input.isThrusting() && speed > 1.0E-3 && player.getData(ModAttachments.FLIGHT_SETTINGS).dampener();
    }
}
