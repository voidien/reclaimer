package com.voidreclaimer.api.weather;

import net.minecraft.server.level.ServerPlayer;

/** What a kind of weather does, in the code registry {@code void_reclaimer:weather_effect_type}. */
@FunctionalInterface
public interface WeatherEffectType {
    /** One second of it on {@code player}. {@code amount} is the effect's strength times the storm's strength times the exposure. */
    void second(ServerPlayer player, float amount);
}
