package com.voidreclaimer.client.render.body;

import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.BodyData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Where a body's exhaust comes from: each installed thruster, so a trail never starts in the air where a part used to be. */
public final class ThrusterExhaust {
    private ThrusterExhaust() {}

    /** Whether any of these mounts holds a thruster: no thruster, no exhaust, in any view. */
    public static boolean installed(Collection<BodyData.Mount> mounts) {
        return mounts.stream().anyMatch(mount -> mount.kind() == ModuleKind.THRUSTER);
    }

    /**
     * The world position of each installed thruster in {@code points}, which are relative to {@code camera} as the world pass draws
     * them. Empty when the body shows no thruster, and the caller then falls back on the middle of the body.
     */
    public static List<Vec3> origins(List<BoneProbe.Point> points, Vec3 camera) {
        List<Vec3> origins = new ArrayList<>();
        for (BoneProbe.Point point : points) {
            if (point.kind() == BoneProbe.Kind.MOUNT && point.filled() && point.mount() != null && point.mount().kind() == ModuleKind.THRUSTER) {
                origins.add(camera.add(point.at().x(), point.at().y(), point.at().z()));
            }
        }
        return origins;
    }
}
