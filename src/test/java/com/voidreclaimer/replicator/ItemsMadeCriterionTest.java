package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.milestone.MilestoneContext;
import com.voidreclaimer.dust.DustData;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ItemsMadeCriterionTest {
    private static final ResourceLocation A = VoidReclaimer.id("a");
    private static final ResourceLocation B = VoidReclaimer.id("b");

    private static MilestoneContext made(Set<ResourceLocation> items) {
        return new MilestoneContext(DustData.EMPTY, 0, Float.MAX_VALUE, 0, items);
    }

    @Test
    void needsEveryItem() {
        MilestoneCriteria.ItemsMade both = new MilestoneCriteria.ItemsMade(List.of(A, B));
        assertFalse(both.isReached(made(Set.of())));
        assertFalse(both.isReached(made(Set.of(A))));
        assertTrue(both.isReached(made(Set.of(A, B))));
        assertTrue(both.isReached(made(Set.of(A, B, VoidReclaimer.id("extra")))));
    }

    @Test
    void olderContextsNeverReachIt() {
        assertFalse(new MilestoneCriteria.ItemsMade(List.of(A)).isReached(MilestoneContext.of(DustData.EMPTY)));
    }

    @Test
    void codecReadsBareIdsAndRejectsAnEmptyList() {
        var codec = MilestoneCriteria.ItemsMade.CODEC.codec();
        var ok = codec.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"items\":[\"a\",\"minecraft:dirt\"]}"));
        assertTrue(ok.result().isPresent());
        assertEquals(List.of(A, ResourceLocation.withDefaultNamespace("dirt")), ok.result().get().items());
        assertTrue(codec.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"items\":[]}")).error().isPresent());
    }

    @Test
    void starshipNeedsOneItemFromEachNebulaChain() {
        var criterion = (MilestoneCriteria.ItemsMade) Milestones.BUILTIN.get(Milestones.STARSHIP).criterion();
        assertEquals(Set.of(VoidReclaimer.id("coolant_frame_plate"), VoidReclaimer.id("carbon_composite_plate"),
                VoidReclaimer.id("high_efficiency_solar_cell"), VoidReclaimer.id("micro_alloy_sheet")), Set.copyOf(criterion.items()));
    }

    @Test
    void dataOfAnOldSaveLoadsWithNoItems() {
        var result = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"blocks_placed\":3}"));
        assertTrue(result.result().isPresent());
        assertTrue(result.result().get().itemsMade().isEmpty());
        assertEquals(3, result.result().get().blocksPlaced());
    }

    @Test
    void rememberingAnItemIsIdempotent() {
        ReplicatorData data = ReplicatorData.EMPTY.withItemMade(A).withItemMade(A).withItemMade(B);
        assertEquals(List.of(A, B), data.itemsMade());
    }
}
