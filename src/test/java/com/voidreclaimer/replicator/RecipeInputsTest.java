package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
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

    @Test
    void aJobKeepsItsItemsThroughASaveAndAdvancing() {
        ReplicatorJob job = new ReplicatorJob(ID, List.of(new ItemStack(Items.STONE)), DustCost.NONE, List.of(new ItemStack(Items.IRON_INGOT, 2)), 0, 9);
        var saved = ReplicatorJob.CODEC.encodeStart(JsonOps.INSTANCE, job).getOrThrow();
        ReplicatorJob loaded = ReplicatorJob.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(1, loaded.items().size());
        assertEquals(2, loaded.items().get(0).getCount());
        assertEquals(1, job.advanced().items().size(), "a job that drops its items while it runs could not refund them");
    }
}
