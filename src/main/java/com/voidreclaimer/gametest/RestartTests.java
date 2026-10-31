package com.voidreclaimer.gametest;

/** For GameTests that span a restart: see {@code tools/restart-test.sh}. */
final class RestartTests {
    /** What {@code tools/restart-test.sh} looks for in the log. */
    private static final String CRASHED = "VR_RESTART_CRASHED";

    private RestartTests() {}

    /**
     * Ends the JVM on the spot, with nothing saved and no shutdown hook run: what a power cut or a JVM crash does. Unlike
     * {@link TestPlayers#rejoin}, which is a clean logout.
     */
    static void crash() {
        System.out.println(CRASHED);
        System.out.flush();
        Runtime.getRuntime().halt(42);
    }
}
