package com.voidreclaimer.api.weather;

import net.minecraft.server.level.ServerPlayer;

/** What shelters a body from a kind of weather, in the code registry {@code void_reclaimer:weather_exposure_type}. */
@FunctionalInterface
public interface WeatherExposureType {
    /** How much of the weather reaches {@code player}, 0 to 1. */
    float exposure(ServerPlayer player);
}
