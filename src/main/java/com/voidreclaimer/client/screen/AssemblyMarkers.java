package com.voidreclaimer.client.screen;

import com.voidreclaimer.client.render.body.BoneProbe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * Which of a body's attachment points the assembly view shows, and which one a click lands on. Sockets show first, and a selected
 * socket adds its mounts; a socket with no point of its own, as on rigs whose sections are not separate bones, shows its mounts
 * straight away. A point on the far side of the body is hidden, so you turn the model to reach it. Pure.
 */
final class AssemblyMarkers {
    private AssemblyMarkers() {}

    /**
     * The points to show.
     *
     * @param all      every point, in GUI pixels: larger z is nearer the viewer
     * @param selected the selected socket, or null
     * @param inFilter whether a socket passes the area filter
     * @param farLimit a point with a smaller z is on the far side
     */
    static List<BoneProbe.Point> visible(List<BoneProbe.Point> all, @Nullable String selected, Predicate<String> inFilter, float farLimit) {
        Set<String> withPoint = new HashSet<>();
        for (BoneProbe.Point point : all) {
            if (point.kind() == BoneProbe.Kind.SOCKET) {
                withPoint.add(point.socket());
            }
        }
        List<BoneProbe.Point> shown = new ArrayList<>();
        for (BoneProbe.Point point : all) {
            boolean relevant = point.kind() == BoneProbe.Kind.SOCKET || point.socket().equals(selected) || !withPoint.contains(point.socket());
            if (relevant && inFilter.test(point.socket()) && point.at().z() >= farLimit) {
                shown.add(point);
            }
        }
        return shown;
    }

    /** The point a click lands on: the closest within {@code radius} GUI pixels, the nearest to the viewer on a tie. */
    static Optional<BoneProbe.Point> pick(List<BoneProbe.Point> shown, double mouseX, double mouseY, double radius) {
        BoneProbe.Point best = null;
        double bestDistance = radius * radius;
        for (BoneProbe.Point point : shown) {
            double dx = point.at().x() - mouseX;
            double dy = point.at().y() - mouseY;
            double distance = dx * dx + dy * dy;
            if (distance < bestDistance || (distance == bestDistance && best != null && point.at().z() > best.at().z())) {
                best = point;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }
}
