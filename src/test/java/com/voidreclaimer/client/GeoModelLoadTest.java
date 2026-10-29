package com.voidreclaimer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.keyframe.BoneAnimation;
import com.google.gson.JsonObject;
import software.bernie.geckolib.loading.json.FormatVersion;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import org.junit.jupiter.api.Test;

/**
 * Every GeckoLib model in the mod loads the way GeckoLib's own cache loads it (GeckoLibCache: GEO_GSON, then
 * GeometryTree, then BakedModelFactory), in the geometry format GeckoLib supports without warnings (1.12.0), and keeps
 * its root bone. Every animation loads the way GeckoLib loads it (FileLoader: the "animations" object through GEO_GSON
 * into BakedAnimations), and every bone it moves exists in the model with the same path under geo/. Catches a broken
 * art drop or a bad `rd_generate.py --promote` before the renderers (5b and later) use them.
 */
class GeoModelLoadTest {

    @Test
    void everyModelLoadsInGeckoLib() throws IOException, URISyntaxException {
        // Through the classpath, like the game: the test's working directory is not the project root.
        URL url = GeoModelLoadTest.class.getClassLoader().getResource("assets/void_reclaimer/geo");
        assertNotNull(url, "assets/void_reclaimer/geo is not on the test classpath");
        Path geo = Path.of(url.toURI());
        List<Path> files;
        try (Stream<Path> walk = Files.walk(geo)) {
            files = walk.filter(p -> p.toString().endsWith(".geo.json")).sorted().toList();
        }
        assertFalse(files.isEmpty(), "no models found under " + geo);
        for (Path file : files) {
            Model model;
            try (Reader reader = Files.newBufferedReader(file)) {
                model = KeyFramesAdapter.GEO_GSON.fromJson(JsonParser.parseReader(reader), Model.class);
            }
            assertEquals(FormatVersion.V_1_12_0, model.formatVersion(),
                    file + ": GeckoLib 4.9.3 only supports geometry 1.12.0 without warnings (no rotated face UVs)");
            BakedGeoModel baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(model));
            assertFalse(baked.topLevelBones().isEmpty(), file + ": no bones");
            String name = file.getFileName().toString().replace(".geo.json", "");
            String folder = file.getParent().getFileName().toString();
            if (folder.equals("section") || folder.equals("module")) {
                assertTrue(baked.getBone(name).isPresent(), file + ": the root bone must be named " + name);
            }
        }
    }

    @Test
    void everyAnimationLoadsInGeckoLibAndMovesRealBones() throws IOException, URISyntaxException {
        URL url = GeoModelLoadTest.class.getClassLoader().getResource("assets/void_reclaimer/animations");
        assertNotNull(url, "assets/void_reclaimer/animations is not on the test classpath");
        Path animations = Path.of(url.toURI());
        Path geo = animations.resolveSibling("geo");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(animations)) {
            files = walk.filter(p -> p.toString().endsWith(".animation.json")).sorted().toList();
        }
        assertFalse(files.isEmpty(), "no animations found under " + animations);
        for (Path file : files) {
            JsonObject json;
            try (Reader reader = Files.newBufferedReader(file)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            BakedAnimations baked = KeyFramesAdapter.GEO_GSON.fromJson(json.getAsJsonObject("animations"), BakedAnimations.class);
            assertFalse(baked.animations().isEmpty(), file + ": no animations");
            Path rel = animations.relativize(file);
            Path model = geo.resolve(rel.toString().replace(".animation.json", ".geo.json"));
            if (!Files.exists(model)) {
                continue;  // shared animations (e.g. for several models) are only checked for loading
            }
            // A body animation moves the assembled body: the chassis rig plus the section and module models attached
            // to it (e.g. drone.rotor_spin turns the Rotor Arm's rotor_spin bone).
            List<Path> models = new java.util.ArrayList<>(List.of(model));
            if (rel.getName(0).toString().equals("body")) {
                for (String part : List.of("section", "module")) {
                    try (Stream<Path> walk = Files.walk(geo.resolve(part))) {
                        walk.filter(p -> p.toString().endsWith(".geo.json")).forEach(models::add);
                    }
                }
            }
            java.util.Set<String> bones = new java.util.HashSet<>();
            for (Path m : models) {
                try (Reader reader = Files.newBufferedReader(m)) {
                    Model raw = KeyFramesAdapter.GEO_GSON.fromJson(JsonParser.parseReader(reader), Model.class);
                    collectBones(BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw))
                            .topLevelBones(), bones);
                }
            }
            for (Animation animation : baked.animations().values()) {
                for (BoneAnimation bone : animation.boneAnimations()) {
                    assertTrue(bones.contains(bone.boneName()), file + ": " + animation.name() + " moves bone "
                            + bone.boneName() + ", which is not in " + model.getFileName()
                            + (models.size() > 1 ? " or any section or module model" : ""));
                }
            }
        }
    }

    private static void collectBones(List<software.bernie.geckolib.cache.object.GeoBone> bones, java.util.Set<String> into) {
        for (software.bernie.geckolib.cache.object.GeoBone bone : bones) {
            into.add(bone.getName());
            collectBones(bone.getChildBones(), into);
        }
    }
}
