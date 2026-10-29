package com.voidreclaimer.dust;

import com.voidreclaimer.registry.ModTags;
import net.minecraft.world.entity.player.Player;

public final class DustSensor {
    private DustSensor() {}

    /**
     * Whether a sensor slot holds an item tagged {@code #void_reclaimer:modules/dust_sensor} (both sides). Carried
     * sensors do nothing.
     */
    public static boolean has(Player player) {
        return com.voidreclaimer.body.Bodies.data(player).hasInstalled(com.voidreclaimer.body.PartKind.SENSOR,
                stack -> stack.is(ModTags.DUST_SENSOR_MODULES));
    }
}
