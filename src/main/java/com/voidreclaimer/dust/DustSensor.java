package com.voidreclaimer.dust;

import com.voidreclaimer.registry.ModItems;
import net.minecraft.world.entity.player.Player;

public final class DustSensor {
    private DustSensor() {}

    /** Whether a Dust Sensor module is installed in the body (both sides). Carried sensors do nothing. */
    public static boolean has(Player player) {
        return com.voidreclaimer.body.Bodies.data(player).hasInstalled(com.voidreclaimer.body.PartKind.SENSOR,
                stack -> stack.is(ModItems.DUST_SENSOR.get()));
    }
}
