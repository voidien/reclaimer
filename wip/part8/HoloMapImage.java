package com.voidreclaimer.display;

import java.util.function.IntBinaryOperator;

/**
 * The picture of a map hologram: {@link #SIZE} x {@link #SIZE} packed map colours, as a vanilla map stores them (0 is nothing).
 * The server fills it from the blocks around a projector; the client adds what moves.
 */
public final class HoloMapImage {
    public static final int SIZE = 64;

    private HoloMapImage() {}

    /** One picture, row by row, from a sampler of pixel x and y. */
    public static byte[] compose(IntBinaryOperator sampler) {
        byte[] image = new byte[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image[y * SIZE + x] = (byte) sampler.applyAsInt(x, y);
            }
        }
        return image;
    }

    /** The offset in blocks from the map's centre of a pixel's middle, on either axis. */
    public static int offset(int pixel, int radius) {
        return (pixel * 2 + 1 - SIZE) * radius / SIZE;
    }

    /** The pixel a block offset falls on, or -1 off the map. */
    public static int pixel(double offset, int radius) {
        double at = (offset + radius) * SIZE / (2.0 * radius);
        return at < 0 || at >= SIZE ? -1 : (int) at;
    }
}
