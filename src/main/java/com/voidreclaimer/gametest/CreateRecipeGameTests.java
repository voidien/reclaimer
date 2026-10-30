package com.voidreclaimer.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voidreclaimer.VoidReclaimer;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every machine recipe we ship (Create chains, Sequenced Assembly, freezing; anything under {@code data/void_reclaimer/recipe}
 * except the Replicator's) must load into the recipe manager, and every item and fluid it names must exist. Create drops a
 * recipe it can't parse with only a log line, so a typo would otherwise make a recipe silently vanish.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateRecipeGameTests {
    private CreateRecipeGameTests() {}

    @GameTest(template = "empty")
    public static void everyMachineRecipeLoadsAndNamesRealItemsAndFluids(GameTestHelper helper) throws IOException, URISyntaxException {
        Path root = Path.of(VoidReclaimer.class.getResource("/data/" + VoidReclaimer.MOD_ID + "/recipe").toURI());
        List<String> problems = new ArrayList<>();
        int checked = 0;
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                if (relative.startsWith("replicator/")) {
                    continue;
                }
                checked++;
                ResourceLocation id = VoidReclaimer.id(relative.substring(0, relative.length() - ".json".length()));
                if (helper.getLevel().getServer().getRecipeManager().byKey(id).isEmpty()) {
                    problems.add(id + " did not load");
                }
                JsonElement json = JsonParser.parseString(Files.readString(file));
                collect(json, id, problems);
            }
        }
        helper.assertTrue(checked >= 20, "only " + checked + " machine recipes found; is the recipe folder missing?");
        helper.assertTrue(problems.isEmpty(), "recipe problems: " + problems);
        helper.succeed();
    }

    /** Walks a recipe JSON and checks the item ids ("item", "id") and fluid ids ("fluid") in it. */
    private static void collect(JsonElement element, ResourceLocation recipe, List<String> problems) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                    if (id != null && !id.getNamespace().equals("neoforge")) {
                        boolean isFluid = entry.getKey().equals("fluid") || (entry.getKey().equals("id") && object.has("amount"));
                        boolean isItem = entry.getKey().equals("item") || (entry.getKey().equals("id") && !object.has("amount") && !object.has("type"));
                        if (isFluid && BuiltInRegistries.FLUID.getOptional(id).isEmpty()) {
                            problems.add(recipe + " names unknown fluid " + id);
                        } else if (isItem && BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                            problems.add(recipe + " names unknown item " + id);
                        }
                    }
                } else {
                    collect(value, recipe, problems);
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collect(child, recipe, problems));
        }
    }
}
