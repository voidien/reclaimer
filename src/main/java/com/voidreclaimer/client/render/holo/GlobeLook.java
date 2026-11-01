package com.voidreclaimer.client.render.holo;

import com.voidreclaimer.earth.Sectors;

/** What each part of a globe looks like (pure): which sector a cell shows, which shell panels stand, and the colours. */
public final class GlobeLook {
    public static final int EARTH_ROWS = 16;
    public static final int EARTH_COLS = 32;
    public static final int SHELL_ROWS = 12;
    public static final int SHELL_COLS = 24;
    /** The most satellites drawn: past this the swarm only gets its count in the caption. */
    public static final int MAX_SATELLITES = 200;

    private static final int RUINED = 0xB0683C;
    private static final int HEALED = 0x3CD07A;
    private static final int STAR = 0xFFE7A0;
    private static final int SHELL = 0xFFC860;
    private static final int[] ATMOSPHERE = {0xC06030, 0x90A060, 0x60B0FF};
    private static final float GOLDEN = 0.618034f;

    private GlobeLook() {}

    /** The planet's sector under a cell of the Earth mesh: columns run west to east and rows north to south, as in {@link Sectors}. */
    public static int sector(int row, int col) {
        return row * Sectors.GRID / EARTH_ROWS * Sectors.GRID + col * Sectors.GRID / EARTH_COLS;
    }

    /** A cell's colour; every other sector is a little darker, so the grid shows. */
    public static int earthColor(int sector, boolean restored) {
        int base = restored ? HEALED : RUINED;
        boolean dark = (Sectors.row(sector) + Sectors.column(sector)) % 2 == 0;
        return dark ? scale(base, 0.75f) : base;
    }

    public static int atmosphere(int era) {
        return ATMOSPHERE[Math.max(0, Math.min(ATMOSPHERE.length - 1, era))];
    }

    public static int star() {
        return STAR;
    }

    public static int shell() {
        return SHELL;
    }

    /** Whether a shell panel stands at this progress. Panels fill in a scattered order, not row by row. */
    public static boolean panelStands(int row, int col, float progress) {
        return (row * SHELL_COLS + col + 1) * GOLDEN % 1f < progress;
    }

    /** How many satellites to draw. */
    public static int satellitesShown(long launched) {
        return (int) Math.min(launched, MAX_SATELLITES);
    }

    /** Where satellite {@code index} is at a time: x, y, z in units of the shell's radius. Each keeps its own tilted orbit. */
    public static float[] satellite(int index, float time) {
        float seed = index * GOLDEN % 1f;
        float angle = (float) (2 * Math.PI * (seed * 7f % 1f)) + time * (0.01f + 0.02f * seed);
        float tilt = (seed - 0.5f) * 2.4f;
        float radius = 1.2f + 0.3f * (seed * 13f % 1f);
        float x = (float) Math.cos(angle) * radius;
        float z = (float) Math.sin(angle) * radius;
        return new float[] {x, z * (float) Math.sin(tilt), z * (float) Math.cos(tilt)};
    }

    private static int scale(int rgb, float by) {
        int r = Math.round((rgb >> 16 & 0xFF) * by);
        int g = Math.round((rgb >> 8 & 0xFF) * by);
        int b = Math.round((rgb & 0xFF) * by);
        return r << 16 | g << 8 | b;
    }
}
