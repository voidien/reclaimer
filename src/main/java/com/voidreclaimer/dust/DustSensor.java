package com.voidreclaimer.dust;

import com.voidreclaimer.registry.ModItems;
import net.minecraft.world.entity.player.Player;

public final class DustSensor {
    private DustSensor() {}

    /** Whether the player carries a Dust Sensor. Phase 5a adds the sensor body module as a second source. */
    public static boolean has(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.DUST_SENSOR.get()));
    }
}
