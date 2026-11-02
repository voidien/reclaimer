package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RecipeInputsTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("void_reclaimer", "replicator/test");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static List<ItemStack> inventory(ItemStack... stacks) {
        return List.of(stacks);
    }

    private static ReplicatorJob jobPaidBy(Optional<UUID> body, boolean items) {
        return new ReplicatorJob(ID, List.of(new ItemStack(Items.STONE)), DustCost.NONE, items ? List.of(new ItemStack(Items.IRON_INGOT)) : List.of(), body, 0, 9);
    }

    @Test
    void onlyTheBodyThatPaidCanCancelAJobThatTookItems() {
        Optional<UUID> shell = Optional.of(UUID.randomUUID());
        Optional<UUID> other = Optional.of(UUID.randomUUID());
        assertTrue(ReplicatorLogic.canCancel(jobPaidBy(shell, true), shell, id -> false), "the shell that paid");
        assertTrue(ReplicatorLogic.canCancel(jobPaidBy(Optional.empty(), true), Optional.empty(), id -> false), "the main body that paid");
        assertFalse(ReplicatorLogic.canCancel(jobPaidBy(shell, true), Optional.empty(), id -> false), "home cancelling a shell's job");
        assertFalse(ReplicatorLogic.canCancel(jobPaidBy(Optional.empty(), true), shell, id -> false), "a shell cancelling a job from home");
        assertFalse(ReplicatorLogic.canCancel(jobPaidBy(shell, true), other, id -> false), "another shell");
    }

    @Test
    void aBodyThatHasGoneCantHoldItsJobsForGood() {
        Optional<UUID> shell = Optional.of(UUID.randomUUID());
        assertTrue(ReplicatorLogic.canCancel(jobPaidBy(shell, true), Optional.empty(), id -> true));
    }

    @Test
    void aJobWithNoItemsCanBeCancelledAnywhere() {
        assertTrue(ReplicatorLogic.canCancel(jobPaidBy(Optional.of(UUID.randomUUID()), false), Optional.empty(), id -> false));
    }

    @Test
    void aRecipeWithNoItemsTakesNothing() {
        assertEquals(RecipeInputs.NOTHING, RecipeInputs.plan(inventory(), List.of()));
    }

    @Test
    void itTakesFromSeveralSlotsAndLeavesTheRest() {
        List<ItemStack> slots = inventory(new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.STONE, 5), new ItemStack(Items.IRON_INGOT, 4));
        RecipeInputs.Plan plan = RecipeInputs.plan(slots, List.of(SizedIngredient.of(Items.IRON_INGOT, 3)));
        assertNotNull(plan);
        assertEquals(2, slots.get(0).getCount(), "planning changes nothing");
        plan.take(slots);
        assertEquals(0, slots.get(0).getCount());
        assertEquals(5, slots.get(1).getCount());
        assertEquals(3, slots.get(2).getCount());
        assertEquals(3, plan.items().stream().mapToInt(ItemStack::getCount).sum());
    }

    @Test
    void heldCountsEverySlot() {
        List<ItemStack> slots = inventory(new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.STONE, 5), new ItemStack(Items.IRON_INGOT, 4));
        assertEquals(6, RecipeInputs.held(slots, SizedIngredient.of(Items.IRON_INGOT, 9)));
    }

    @Test
    void oneStackCannotPayTwoInputs() {
        List<ItemStack> slots = inventory(new ItemStack(Items.IRON_INGOT, 3));
        assertNull(RecipeInputs.plan(slots, List.of(SizedIngredient.of(Items.IRON_INGOT, 2), SizedIngredient.of(Items.IRON_INGOT, 2))));
    }

    @Test
    void oneMissingInputRefusesTheWholeRecipe() {
        List<ItemStack> slots = inventory(new ItemStack(Items.IRON_INGOT, 9));
        assertNull(RecipeInputs.plan(slots, List.of(SizedIngredient.of(Items.IRON_INGOT, 1), SizedIngredient.of(Items.STONE, 1))));
    }

    @Test
    void aJobSavedBeforeItemRecipesLoadsWithNoItems() {
        var old = JsonParser.parseString("""
                {"recipe": "void_reclaimer:replicator/test", "results": [{"id": "minecraft:stone", "count": 1}], "progress": 3, "total": 9}
                """);
        ReplicatorJob job = ReplicatorJob.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
        assertTrue(job.items().isEmpty());
        assertEquals(3, job.progress());
    }

    private static final String GOOD_JOB = """
            {"recipe": "void_reclaimer:replicator/good", "results": [{"id": "minecraft:stone", "count": 1}], "items": [{"id": "minecraft:iron_ingot", "count": 2}], "total": 9}
            """;

    @Test
    void anItemFromAGoneModCostsThatItemAndNotTheQueue() {
        String bad = """
                {"recipe": "void_reclaimer:replicator/bad", "results": [{"id": "gone_mod:thing", "count": 1}],
                 "items": [{"id": "minecraft:iron_ingot", "count": 1}, {"id": "gone_mod:part", "count": 1}], "total": 9}
                """;
        String unreadable = "{\"recipe\": 5}";
        var saved = JsonParser.parseString("{\"jobs\": [" + GOOD_JOB + "," + unreadable + "," + bad + "], \"buffer\": [{\"id\": \"minecraft:stone\", \"count\": 1}, {\"id\": \"gone_mod:thing\", \"count\": 1}]}");
        ReplicatorData data = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(2, data.jobs().size(), "a bad item took a job with it, or a job that can't be read took the queue");
        assertEquals(1, data.jobs().get(0).items().size(), "the good job lost its items");
        assertEquals(1, data.jobs().get(1).items().size(), "the bad job lost the item it could still read");
        assertTrue(data.jobs().get(1).results().isEmpty(), "a result that has gone stays gone");
        assertEquals(1, data.buffer().size(), "the output lost a good item to a bad one");
    }

    @Test
    void aJobKeepsItsItemsThroughASaveAndAdvancing() {
        ReplicatorJob job = new ReplicatorJob(ID, List.of(new ItemStack(Items.STONE)), DustCost.NONE, List.of(new ItemStack(Items.IRON_INGOT, 2)), Optional.empty(), 0, 9);
        var saved = ReplicatorJob.CODEC.encodeStart(JsonOps.INSTANCE, job).getOrThrow();
        ReplicatorJob loaded = ReplicatorJob.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(1, loaded.items().size());
        assertEquals(2, loaded.items().get(0).getCount());
        assertEquals(1, job.advanced().items().size(), "a job that drops its items while it runs could not refund them");
    }
}
