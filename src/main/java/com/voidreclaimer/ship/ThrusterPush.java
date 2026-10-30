package com.voidreclaimer.ship;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3d;

/** Queues a thruster's push on the ship it is built into. Both are in the ship's own coordinates, like Sable's propeller. */
final class ThrusterPush {
    private ThrusterPush() {}

    /** Pushes {@code subLevel} along {@code direction} from the middle of the block at {@code pos}. */
    static void apply(ServerSubLevel subLevel, BlockPos pos, Direction direction, double force, double dt) {
        Vector3d impulse = new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ()).mul(force * dt);
        Vector3d at = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).applyAndRecordPointForce(at, impulse);
    }
}
