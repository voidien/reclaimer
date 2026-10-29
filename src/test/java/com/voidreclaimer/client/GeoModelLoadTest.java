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
import software.bernie.geckolib.loading.json.FormatVersion;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import org.junit.jupiter.api.Test;

/**
 * Every GeckoLib model in the mod loads the way GeckoLib's own cache loads it (GeckoLibCache: GEO_GSON, then
 * GeometryTree, then BakedModelFactory), in the geometry format GeckoLib supports without warnings (1.12.0), and keeps
 * its root bone. Catches a broken art drop or a bad `rd_generate.py --promote` before body rendering (5b) uses them.
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
}
