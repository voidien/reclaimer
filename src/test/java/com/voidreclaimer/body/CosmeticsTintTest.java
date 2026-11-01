package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class CosmeticsTintTest {
    @Test
    void anOlderSaveHasNoTint() {
        assertEquals(Cosmetics.NO_TINT, Cosmetics.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow().tint());
    }

    @Test
    void aTintComesBackFromASaveAndLeavesTheRestAlone() {
        Cosmetics tinted = Cosmetics.DEFAULT.withTint(0xFF6B6B).withAccent(0x7CFC8A).withCasing(Cosmetics.BRASS).withTrail(Cosmetics.EMBER);
        assertEquals(0xFF6B6B, tinted.tint(), "the other settings dropped the tint");
        assertEquals(0x7CFC8A, tinted.withTint(0xFFFFFF).accent(), "a tint changed the accent");
        assertEquals(tinted, Cosmetics.CODEC.parse(JsonOps.INSTANCE, Cosmetics.CODEC.encodeStart(JsonOps.INSTANCE, tinted).getOrThrow()).getOrThrow());
    }
}
