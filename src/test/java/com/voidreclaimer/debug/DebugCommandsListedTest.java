package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every debug command, photo key, test scene and script is in {@code docs/debug-commands.md}. A body remove command sat unlisted for
 * months and work was done by hand that it could have done. This reads the source, so a new one is caught however it is added.
 */
class DebugCommandsListedTest {
    private static final Path ROOT = root();
    private static final String LIST = "docs/debug-commands.md";

    private static Path root() {
        Path path = Path.of("").toAbsolutePath();
        while (!Files.exists(path.resolve("CLAUDE.md"))) {
            path = path.getParent();
        }
        return path;
    }

    private static String list() throws IOException {
        return Files.readString(ROOT.resolve(LIST));
    }

    private static Set<String> notListed(Set<String> names) throws IOException {
        String list = list().toLowerCase(Locale.ROOT);
        Set<String> missing = new TreeSet<>();
        for (String name : names) {
            if (!Pattern.compile("(?<![\\w-])" + Pattern.quote(name.toLowerCase(Locale.ROOT)) + "(?![\\w-])").matcher(list).find()) {
                missing.add(name);
            }
        }
        return missing;
    }

    private static Set<String> found(Pattern pattern, String text) {
        Set<String> names = new TreeSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @Test
    void everyWordOfEveryCommandIsListed() throws IOException {
        Pattern literal = Pattern.compile("Commands\\.literal\\(\"([a-z_0-9]+)\"\\)");
        Set<String> words = new TreeSet<>();
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                words.addAll(found(literal, Files.readString(file)));
            }
        }
        assertTrue(words.size() > 40, "the scan found only " + words + ", so it has stopped working");
        assertEquals(Set.of(), notListed(words), "commands missing from " + LIST);
    }

    @Test
    void everyPhotoKeyIsListed() throws IOException {
        String screen = Files.readString(ROOT.resolve("src/main/java/com/voidreclaimer/client/camera/PhotoModeScreen.java"));
        Set<String> keys = found(Pattern.compile("keyCode == GLFW\\.GLFW_KEY_([A-Z0-9_]+)"), screen);
        assertTrue(keys.size() > 5, "the scan found only " + keys + ", so it has stopped working");
        Set<String> missing = new TreeSet<>();
        for (String key : keys) {
            if (!list().contains("`" + key + "`")) {
                missing.add(key);
            }
        }
        assertEquals(Set.of(), missing, "photo keys missing from " + LIST + ", in backticks");
    }

    @Test
    void everyTestSceneIsListed() throws IOException {
        Set<String> scenes = new TreeSet<>();
        for (TestScenes.Scene scene : TestScenes.Scene.values()) {
            scenes.add(scene.id());
        }
        assertEquals(Set.of(), notListed(scenes), "test scenes missing from " + LIST);
    }

    @Test
    void everyQuickCommandIsListed() throws IOException {
        Set<String> commands = found(Pattern.compile("(?m)^([a-z]+)\\)"), Files.readString(ROOT.resolve("tools/client-test/quick.sh")));
        assertTrue(commands.size() > 10, "the scan found only " + commands + ", so it has stopped working");
        assertEquals(Set.of(), notListed(commands), "quick.sh commands missing from " + LIST);
    }

    @Test
    void everyScriptIsListed() throws IOException {
        Set<String> scripts = new TreeSet<>();
        for (String folder : new String[] {"tools", "tools/client-test"}) {
            try (Stream<Path> files = Files.list(ROOT.resolve(folder))) {
                files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                        .filter(name -> !name.startsWith("test_") || name.endsWith(".sh"))
                        .forEach(scripts::add);
            }
        }
        assertTrue(scripts.size() > 10, "the scan found only " + scripts + ", so it has stopped working");
        assertEquals(Set.of(), notListed(scripts), "scripts missing from " + LIST);
    }
}
