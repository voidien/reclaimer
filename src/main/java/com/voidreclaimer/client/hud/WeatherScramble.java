package com.voidreclaimer.client.hud;

import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.weather.WeatherState;
import net.minecraft.client.multiplayer.ClientLevel;

/** How much weather is scrambling sensors and the HUD in {@code level} just now, 0 to 1. */
final class WeatherScramble {
    private WeatherScramble() {}

    static float of(ClientLevel level) {
        WeatherState state = level.getData(ModAttachments.WEATHER);
        if (!state.active()) {
            return 0f;
        }
        return state.weather().flatMap(level.registryAccess().registryOrThrow(VoidReclaimerRegistries.SPACE_WEATHER)::getOptional)
                .filter(weather -> weather.scrambles()).map(weather -> state.strength()).orElse(0f);
    }
}
