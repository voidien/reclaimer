package com.voidreclaimer.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.Config;
import org.junit.jupiter.api.Test;

class ConfigConditionTest {
    @Test
    void codecRoundTripsTheKey() {
        ConfigCondition condition = new ConfigCondition("iceChain");
        var json = ConfigCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, condition).getOrThrow();
        assertEquals("iceChain", ((JsonObject) json).get("key").getAsString());
        assertEquals(condition, ConfigCondition.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void everyToggleDefaultsOnAndUnknownOnesCountAsOn() {
        assertTrue(Config.TOGGLES.size() >= 6);
        Config.TOGGLES.keySet().forEach(key -> assertTrue(Config.isEnabled(key), key));
        assertTrue(Config.isEnabled("no_such_toggle"));
    }
}
