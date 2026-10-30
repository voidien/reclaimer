package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voidreclaimer.VoidReclaimer;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RecipeDescriptionTest {
    @Test
    void theKeyIsDerivedFromTheRecipeId() {
        assertEquals("replicator.void_reclaimer.description.dust_sensor",
                ReplicatorRecipe.descriptionKey(VoidReclaimer.id("replicator/dust_sensor")));
        assertEquals("replicator.addon.description.plasma_glass",
                ReplicatorRecipe.descriptionKey(ResourceLocation.fromNamespaceAndPath("addon", "replicator/plasma_glass")));
    }

    @Test
    void everyBuiltInRecipeHasADescriptionLine() throws Exception {
        JsonObject lang;
        try (Reader in = new InputStreamReader(RecipeDescriptionTest.class.getResourceAsStream("/assets/void_reclaimer/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(in).getAsJsonObject();
        }
        List<String> missing = new ArrayList<>();
        java.nio.file.Path dir = java.nio.file.Path.of(RecipeDescriptionTest.class.getResource("/data/void_reclaimer/recipe/replicator").toURI());
        try (var files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path file : files.toList()) {
                String name = file.getFileName().toString().replace(".json", "");
                JsonElement line = lang.get(ReplicatorRecipe.descriptionKey(VoidReclaimer.id("replicator/" + name)));
                if (line == null || line.getAsString().isBlank()) {
                    missing.add(name);
                }
            }
        }
        assertTrue(missing.isEmpty(), "recipes without a description: " + missing);
    }
}
