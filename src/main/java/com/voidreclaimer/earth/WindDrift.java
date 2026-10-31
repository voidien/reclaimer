package com.voidreclaimer.earth;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.FlightPhysics;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Drifts every ship over ruined Earth with the wind, once per server tick: the ship's horizontal velocity moves a
 * fraction of the way toward the wind's each tick (the same coupling as the body's, {@link FlightPhysics#WIND_COUPLING}),
 * so a free hull ends up carried at wind speed and a powered one feels a steady lean. Sable has a wind hook of its own
 * ({@code SubLevelHelper.registerWindProvider}), but nothing in Sable, Aeronautics or Propulsion reads it, so the push
 * is applied here through the rigid body. Deep space has no wind.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class WindDrift {
    private WindDrift() {}

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        double maxSpeed = EarthWind.maxSpeedPerSecond();
        ServerLevel earth = maxSpeed > 0.0 ? event.getServer().getLevel(RuinedEarth.DIMENSION) : null;
        if (earth == null) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(earth);
        if (container == null) {
            return;
        }
        for (ServerSubLevel ship : container.getAllSubLevels()) {
            if (ship.isRemoved()) {
                continue;
            }
            RigidBodyHandle body = RigidBodyHandle.of(ship);
            if (body == null || !body.isValid()) {
                continue;
            }
            Vector3dc at = ship.logicalPose().position();
            Vec3 wind = EarthWind.field(at.x(), at.z(), earth.getGameTime(), maxSpeed);
            Vector3d velocity = body.getLinearVelocity(new Vector3d());
            Vector3d nudge = new Vector3d((wind.x - velocity.x) * FlightPhysics.WIND_COUPLING, 0.0, (wind.z - velocity.z) * FlightPhysics.WIND_COUPLING);
            body.addLinearAndAngularVelocity(nudge, new Vector3d());
        }
    }
}
