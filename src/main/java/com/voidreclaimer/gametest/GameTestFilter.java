package com.voidreclaimer.gametest;

import java.util.Arrays;
import java.util.Locale;

/**
 * Picks GameTests by name for {@code ./gradlew runGameTestServer -PgametestFilter=<terms>}. Terms are separated by
 * commas; a test runs when its class name or method name contains any term, ignoring case.
 */
public final class GameTestFilter {
    public static final String PROPERTY = "void_reclaimer.gametestFilter";

    private GameTestFilter() {}

    public static boolean matches(String filter, String className, String methodName) {
        String cls = className.toLowerCase(Locale.ROOT);
        String method = methodName.toLowerCase(Locale.ROOT);
        return Arrays.stream(filter.split(","))
                .map(term -> term.strip().toLowerCase(Locale.ROOT))
                .filter(term -> !term.isEmpty())
                .anyMatch(term -> cls.contains(term) || method.contains(term));
    }
}
