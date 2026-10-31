package com.voidreclaimer.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** What the Helm needs from any thruster block entity: where it pushes from and to, and a throttle it can set. */
public interface ThrusterLike {
    BlockPos getBlockPos();

    /** The way it pushes the ship (ship frame): away from its nozzle. */
    Direction pushDirection();

    /** Force at full throttle, with what it has right now (fuel, power, spin). */
    double maxForce();

    /** The force it pushes with right now (0 when not firing). */
    double force();

    /**
     * A throttle set by a Helm this tick, 0 to 1, which beats redstone until the next tick. Set every tick while a pilot
     * sits; a thruster that is not told anything falls back to redstone.
     */
    void setCommandedThrottle(double throttle);
}
