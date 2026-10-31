package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ItemCensusTest {
    private static final ResourceLocation STONE = ResourceLocation.withDefaultNamespace("stone");
    private static final ResourceLocation DIRT = ResourceLocation.withDefaultNamespace("dirt");
    private static final ResourceLocation GOLD = ResourceLocation.withDefaultNamespace("gold_ingot");

    @Test
    void twoPlacesAddUp() {
        ItemCensus both = new ItemCensus(Map.of(STONE, 3, DIRT, 1)).plus(new ItemCensus(Map.of(STONE, 2)));
        assertEquals(Map.of(STONE, 5, DIRT, 1), both.counts());
        assertEquals(6, both.total());
    }

    @Test
    void aChangeSaysWhatAppearedAndWhatVanished() {
        ItemCensus before = new ItemCensus(Map.of(STONE, 3, DIRT, 1));
        ItemCensus after = new ItemCensus(Map.of(STONE, 5, GOLD, 2));
        assertEquals(Map.of(STONE, 2, GOLD, 2, DIRT, -1), after.since(before));
    }

    @Test
    void nothingChangedSaysNothing() {
        ItemCensus same = new ItemCensus(Map.of(STONE, 3));
        assertTrue(new ItemCensus(Map.of(STONE, 3)).since(same).isEmpty());
    }

    @Test
    void changesComeInAFixedOrder() {
        ItemCensus after = new ItemCensus(Map.of(STONE, 1, DIRT, 1, GOLD, 1));
        assertEquals(List.of(DIRT, GOLD, STONE), List.copyOf(after.since(new ItemCensus(Map.of())).keySet()));
    }
}
