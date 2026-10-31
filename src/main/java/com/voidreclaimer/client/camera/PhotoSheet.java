package com.voidreclaimer.client.camera;

import java.util.List;

/** The four views of a photo sheet, relative to a subject's facing, pure. */
public final class PhotoSheet {
    /**
     * One view.
     *
     * @param suffix added to the sheet's name for the file
     * @param yaw    degrees from looking at the subject's front
     * @param pitch  degrees, positive looks down from above
     */
    public record View(String suffix, float yaw, float pitch) {}

    public static final List<View> VIEWS = List.of(new View("front", 0f, 8f), new View("side", 90f, 8f),
            new View("three_quarter", 40f, 25f), new View("top", 0f, 75f));

    private PhotoSheet() {}

    /** The camera yaw for looking at the subject's front: facing back the way it faces. */
    public static float frontYaw(float subjectYaw) {
        return subjectYaw + 180f;
    }
}
