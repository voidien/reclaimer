package com.voidreclaimer.display;

import java.util.List;

/**
 * The picture of a network hologram: a level's relays round a projector, lit or dark, and what the lit ones reach. It zooms out to show
 * them all. Pure, so it can be unit tested; the client adds dust and what moves, as on any map.
 */
public final class HoloNetworkMap {
    /** The closest a network map gets, in blocks from its middle to its edge. */
    static final int SMALLEST_RADIUS = 256;
    /** As far as a map goes: {@link HoloMapImage#offset} would overflow beyond this. */
    static final int LARGEST_RADIUS = 16_000_000;
    /** Room round the furthest relay, so that it isn't on the very edge. */
    private static final double MARGIN = 1.1;

    /** A relay, where it is in the world. */
    public record Node(double x, double z, boolean lit) {}

    /** Packed map colours. */
    public record Colours(int here, int lit, int dark, int reach) {}

    private HoloNetworkMap() {}

    /** How far out a map centred at {@code x}, {@code z} has to go, in blocks. */
    public static int radius(List<Node> relays, double x, double z) {
        double furthest = 0.0;
        for (Node node : relays) {
            furthest = Math.max(furthest, Math.max(Math.abs(node.x() - x), Math.abs(node.z() - z)));
        }
        return (int) Math.min(LARGEST_RADIUS, Math.max(SMALLEST_RADIUS, Math.ceil(furthest * MARGIN)));
    }

    /** The picture, centred at {@code x}, {@code z}. A lit relay reaches {@code range} blocks; the dark ones come first, so a lit one is never hidden. */
    public static byte[] draw(List<Node> relays, double x, double z, int radius, double range, Colours colours) {
        byte[] image = HoloMapImage.compose((pixelX, pixelY) -> reached(relays, x + HoloMapImage.offset(pixelX, radius), z + HoloMapImage.offset(pixelY, radius), range) ? colours.reach() : 0);
        for (Node node : relays) {
            if (!node.lit()) {
                mark(image, node.x() - x, node.z() - z, radius, colours.dark());
            }
        }
        for (Node node : relays) {
            if (node.lit()) {
                mark(image, node.x() - x, node.z() - z, radius, colours.lit());
            }
        }
        mark(image, 0.0, 0.0, radius, colours.here());
        return image;
    }

    private static boolean reached(List<Node> relays, double x, double z, double range) {
        for (Node node : relays) {
            double east = x - node.x();
            double south = z - node.z();
            if (node.lit() && east * east + south * south <= range * range) {
                return true;
            }
        }
        return false;
    }

    /** A small cross at the place, so that one relay on a 64 pixel map can be seen. What falls off the map is left out. */
    private static void mark(byte[] image, double east, double south, int radius, int colour) {
        int x = HoloMapImage.pixel(east, radius);
        int y = HoloMapImage.pixel(south, radius);
        if (x >= 0 && y >= 0) {
            paint(image, x, y, colour);
            paint(image, x - 1, y, colour);
            paint(image, x + 1, y, colour);
            paint(image, x, y - 1, colour);
            paint(image, x, y + 1, colour);
        }
    }

    private static void paint(byte[] image, int x, int y, int colour) {
        if (x >= 0 && y >= 0 && x < HoloMapImage.SIZE && y < HoloMapImage.SIZE) {
            image[y * HoloMapImage.SIZE + x] = (byte) colour;
        }
    }
}
