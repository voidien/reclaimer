package com.voidreclaimer.dust;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import org.junit.jupiter.api.Test;

class CollectorsTest {
    @Test
    void wholeItemsComeOutAndTheFractionIsCarried() {
        Collectors.Step step = Collectors.accumulate(0.75f, 0.5f);
        assertEquals(1, step.whole());
        assertEquals(0.25f, step.carry(), 1e-6f);
        assertEquals(0, Collectors.accumulate(0f, 0.4f).whole());
    }

    @Test
    void manySmallGainsAddUpToWholeItems() {
        float carry = 0;
        int items = 0;
        for (int i = 0; i < 100; i++) {
            Collectors.Step step = Collectors.accumulate(carry, 0.045f);
            items += step.whole();
            carry = step.carry();
        }
        assertEquals(4, items); // 4.5 items' worth
    }

    @Test
    void negativeProductionNeverTakesItemsAway() {
        assertEquals(0.5f, Collectors.accumulate(0.5f, -3f).carry(), 1e-6f);
    }

    @Test
    void producedScalesWithYieldAndMultiplier() {
        assertEquals(3f, Collectors.produced(2f, 1.5f, 1.0), 1e-6f);
        assertEquals(1f, Collectors.produced(2f, 1.0f, 0.5), 1e-6f);
        assertEquals(0f, Collectors.produced(2f, 1.0f, 0.0), 1e-6f);
    }

    @Test
    void collectorSpecReadsFromDataAndOldModulesHaveNone() {
        var json = JsonParser.parseString("""
                {"kind":"tool","collector":{"yield":2.0,"energy_per_second":0.5,"outputs":{"metallic":"void_reclaimer:metallic_grains"}}}""");
        ModuleStats stats = ModuleStats.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(ModuleKind.TOOL, stats.kind());
        assertEquals(2.0f, stats.collector().orElseThrow().yield());
        assertEquals("void_reclaimer:metallic_grains", stats.collector().orElseThrow().outputs().get(DustKinds.METALLIC).toString());

        var old = JsonParser.parseString("{\"kind\":\"sensor\"}");
        assertEquals(java.util.Optional.empty(), ModuleStats.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow().collector());
    }
}
