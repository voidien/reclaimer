package com.voidreclaimer.gametest;

import java.util.Arrays;
import java.util.Locale;

/**
 * Picks GameTests by name for {@code ./gradlew runGameTestServer -PgametestFilter=<terms>}. Terms are separated by
 * commas; a test runs when its class name or method name contains any term, ignoring case. A test that spans a restart
 * only makes sense in its place in {@code tools/restart-test.sh}, so it runs only when a term names its phase.
 */
public final class GameTestFilter {
    public static final String PROPERTY = "void_reclaimer.gametestFilter";
    private static final String BEFORE_RESTART = "beforerestart";
    private static final String AFTER_RESTART = "afterrestart";

    private GameTestFilter() {}

    /** Whether the test should be registered, with {@code filter} possibly null or blank for a full run. */
    public static boolean registers(String filter, String className, String methodName) {
        String method = methodName.toLowerCase(Locale.ROOT);
        boolean none = filter == null || filter.isBlank();
        if (method.endsWith(BEFORE_RESTART) || method.endsWith(AFTER_RESTART)) {
            return !none && Arrays.stream(filter.split(",")).map(term -> term.strip().toLowerCase(Locale.ROOT)).anyMatch(method::equals);
        }
        return none || matches(filter, className, methodName);
    }

    public static boolean matches(String filter, String className, String methodName) {
        String cls = className.toLowerCase(Locale.ROOT);
        String method = methodName.toLowerCase(Locale.ROOT);
        return Arrays.stream(filter.split(","))
                .map(term -> term.strip().toLowerCase(Locale.ROOT))
                .filter(term -> !term.isEmpty())
                .anyMatch(term -> cls.contains(term) || method.contains(term));
    }
}
