package com.voidreclaimer.flight;

import com.voidreclaimer.Config;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ZeroGravity;
import com.voidreclaimer.tether.Tether;
import com.voidreclaimer.tether.TetherData;
import com.voidreclaimer.tether.TetherPhysics;
import com.voidreclaimer.earth.RuinedEarth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Zero-G thruster flight shared by both sides.
 *
 * <p>Thrust is relative to the view: forward/back along the look vector, strafe along the view's right vector
 * (always horizontal, since there is no roll), up/down along the view's up vector. With the inertial dampener on,
 * the body brakes to a stop whenever no thrust key is held, and while one is held it brakes the drift off the
 * thrust's direction, so velocity follows the nose. There is no drag otherwise, so with the dampener off the body
 * drifts until it thrusts the other way. Speed is hard-capped by {@link FlightStats#maxSpeed()}. In low
 * power mode thrusters and dampener are offline and the body drifts. A clipped Tether Anchor leashes the body, and
 * the tether recall winch pulls it toward the anchor faster than top speed.
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
        if (!ZeroGravity.isZeroG(player.level(), player.blockPosition())
                && (!canHover(player) || walks(player) && !player.getData(ModAttachments.FLIGHT_INPUT).engaged())) {
            return false;
        }
        return !player.isCreative() || player.getData(ModAttachments.FLIGHT_SETTINGS).creativeThrusters();
    }

    /**
     * Whether the body flies on thrusters where gravity is back (ruined Earth): any body with thrust at all. A body
     * with no thrust cannot hold itself up, so it walks like a normal player instead of being stuck.
     */
    public static boolean canHover(Player player) {
        return RuinedEarth.is(player.level()) && FlightStats.of(player).thrust() > 0.0;
    }

    /** A body with legs (the Mech) on ground with gravity: it walks, and its thrusters fire only while engaged. */
    public static boolean walks(Player player) {
        return com.voidreclaimer.body.Bodies.walks(player);
    }

    /** Whether this body could fire its thrusters here but is walking until they are engaged. */
    public static boolean canEngage(Player player) {
        return canHover(player) && walks(player) && !player.isSpectator() && !player.isPassenger()
                && (!player.isCreative() || player.getData(ModAttachments.FLIGHT_SETTINGS).creativeThrusters());
    }

    /** The downward acceleration (blocks/tick²) the thrusters must cancel here: 0 in zero-G, else gravity times the config factor. */
    public static double weight(Player player) {
        if (ZeroGravity.isZeroG(player.level(), player.blockPosition())) {
            return 0.0;
        }
        return player.getAttributeValue(Attributes.GRAVITY) * Config.HOVER_WEIGHT_FACTOR.get();
    }

    /**
     * The speed that went into what was hit: what the hit boxes took, and what vanilla's box took on an axis it collided on. The dampener's
     * share went in {@code step}, and vanilla also slows a body in a cobweb or on soul sand, which is no crash.
     */
    private static Vec3 lostToWhatWasHit(Player player, Vec3 wanted, Vec3 kept) {
        Vec3 after = player.getDeltaMovement();
        Vec3 byVanilla = new Vec3(player.horizontalCollision ? kept.x - after.x : 0.0, player.verticalCollision ? kept.y - after.y : 0.0,
                player.horizontalCollision ? kept.z - after.z : 0.0);
        return wanted.subtract(kept).add(byVanilla);
    }

    /** Hears a body losing more speed than is safe. Installed by the client entrypoint, which keeps this class free of client-only references. */
    public static volatile java.util.function.Consumer<Vec3> crashed = lost -> {};

    /** Replaces vanilla travel for the local player: apply thrust, then move with collisions and no drag. */
    public static void travel(Player player) {
        // A big body turns first, so thrust goes where it now points. A push off a wall is a move, not a speed: it stays out of the momentum.
        Vec3 push = com.voidreclaimer.body.BodyTurning.turn(player);
        if (push.lengthSqr() > 0.0) {
            player.move(MoverType.SELF, push);
        }
        Vec3 wanted = step(player, player.getDeltaMovement(), player.getData(ModAttachments.FLIGHT_INPUT));
        com.voidreclaimer.body.BodyBoxes.Moved moved = com.voidreclaimer.body.BoxCollision.move(player, wanted);
        player.setDeltaMovement(moved.speed());
        player.move(MoverType.SELF, moved.move());
        Vec3 lost = lostToWhatWasHit(player, wanted, moved.speed());
        if (lost.length() * FlightStats.TICKS_PER_SECOND > Config.CRASH_SAFE_SPEED.get() && !Tether.isRecalling(player)) {
            crashed.accept(lost);
        }
        player.calculateEntityAnimation(true);
        calmWalkBob(player);
    }

    /**
     * Touching a wall or floor sets {@code onGround}, which makes vanilla's walking view-bob (and limb swing) kick in,
     * so a drone resting against the floor of a tunnel would bob. Thrusters never walk: keep the bob at zero.
     * Runs after vanilla's per-tick bob update, so the rendered bob is zero.
     */
    public static void calmWalkBob(Player player) {
        player.bob = 0f;
        player.oBob = 0f;
    }

    /** One tick of thruster physics for this player, plus the tether: returns the new velocity. */
    public static Vec3 step(Player player, Vec3 velocity, FlightInput input) {
        FlightStats stats = FlightStats.of(player);
        com.voidreclaimer.body.BodyHeading heading = com.voidreclaimer.body.BodyTurning.of(player);
        Vec3 next = FlightPhysics.step(
                velocity,
                FlightPhysics.thrustDirection(heading.pitch(), heading.yaw(), input),
                Energy.canPowerSystems(player),
                player.getData(ModAttachments.FLIGHT_SETTINGS).dampener(),
                stats,
                cloudDrag(player),
                weight(player),
                com.voidreclaimer.earth.EarthWind.at(player.level(), player.position()));
        return tether(player, next, stats);
    }

    /** The recall winch overrides thrust; otherwise a clipped tether acts as a leash. */
    private static Vec3 tether(Player player, Vec3 velocity, FlightStats stats) {
        Vec3 anchor = Tether.anchorPoint(player);
        if (anchor == null) {
            return velocity;
        }
        TetherData data = Tether.data(player);
        Vec3 center = player.getBoundingBox().getCenter();
        if (data.recalling()) {
            return TetherPhysics.recall(velocity, anchor.subtract(center), Tether.recallSpeed(player),
                    stats.acceleration() * Config.RECALL_SPEED_FACTOR.get());
        }
        if (data.clipped()) {
            return FlightPhysics.capSpeed(TetherPhysics.leash(velocity, center.subtract(anchor), Config.TETHER_LENGTH.get()),
                    stats.maxSpeed());
        }
        return velocity;
    }

    /** Drag from the dust cloud around the player; the same on client and server (shared seed). */
    static FlightPhysics.CloudDrag cloudDrag(Player player) {
        DustField field = DustField.of(player.level());
        if (field == null) {
            return FlightPhysics.CloudDrag.NONE;
        }
        double density = field.sample(player.getBoundingBox().getCenter()).density();
        if (density <= 0.0) {
            return FlightPhysics.CloudDrag.NONE;
        }
        return new FlightPhysics.CloudDrag(density, Config.CLOUD_DRAG.get(), Config.CLOUD_SPEED_PENALTY.get());
    }

    /** Whether the dampener is braking this tick (for energy cost). */
    public static boolean isBraking(Player player, FlightInput input, double speed) {
        return !input.isThrusting() && speed > 1.0E-3 && player.getData(ModAttachments.FLIGHT_SETTINGS).dampener();
    }
}
