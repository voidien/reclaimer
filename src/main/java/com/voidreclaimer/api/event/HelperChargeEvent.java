package com.voidreclaimer.api.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

/**
 * Fired on the game bus (server side) about once a second for every Helper Drone, to decide what its battery takes in.
 * {@code charge} is per tick and starts as what sunlight and a Station Core in range give; an addon with another way to
 * charge a drone (a charging pad, say) raises it.
 */
public class HelperChargeEvent extends Event {
    private final Entity drone;
    private float charge;

    public HelperChargeEvent(Entity drone, float charge) {
        this.drone = drone;
        this.charge = charge;
    }

    /** The drone asking. */
    public Entity getDrone() {
        return drone;
    }

    public float getCharge() {
        return charge;
    }

    /** Replaces the charge per tick; negative values count as zero. */
    public void setCharge(float charge) {
        this.charge = Math.max(0f, charge);
    }
}
