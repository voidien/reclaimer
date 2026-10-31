package com.voidreclaimer.client.render.body;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/**
 * Where a body's sockets and mounts really are in the frame being drawn, read from the render matrices so that animation and
 * scale are included. While capturing, {@link BodyPartsLayer} adds a point for each one the local player's body draws.
 * {@code at} is what the render matrix gives: camera-relative blocks in the world, GUI pixels in a screen.
 */
public final class BoneProbe {
    public enum Kind { SOCKET, MOUNT }

    /**
     * One attachment point.
     *
     * @param name   the socket id, or the rig's mount name such as {@code head__mount_sensor_1}
     * @param filled whether something is installed there
     */
    public record Point(String name, Kind kind, boolean filled, Vector3f at) {}

    private static boolean capturing;
    private static boolean recording;
    private static List<Point> building = new ArrayList<>();
    private static List<Point> latest = List.of();

    private BoneProbe() {}

    /** Turns capturing on or off; off forgets the last frame's points. */
    public static void capture(boolean on) {
        capturing = on;
        if (!on) {
            latest = List.of();
        }
    }

    static void begin() {
        recording = capturing;
        if (recording) {
            building = new ArrayList<>();
        }
    }

    static void end() {
        if (recording) {
            latest = List.copyOf(building);
        }
        recording = false;
    }

    static boolean recording() {
        return recording;
    }

    static void add(String name, Kind kind, boolean filled, Vector3f at) {
        building.add(new Point(name, kind, filled, at));
    }

    /** The points of the last frame the body was drawn in. */
    public static List<Point> latest() {
        return latest;
    }
}
