package com.voidreclaimer.body.assembly;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.Socket;
import javax.annotation.Nullable;

/** Left and right pairs of sockets, found from the layout so a datapack chassis has them too, pure. */
public final class BodyPairs {
    private static final double SAME = 1.0E-6;

    private BodyPairs() {}

    /**
     * The socket on the other side of the body, or null for one on the centre line. Two sockets pair when they take the same parts and
     * mirror each other across the body: off a pair of parents at the same spot, or off one parent at opposite sides of it.
     */
    @Nullable
    public static Socket partner(Chassis chassis, String socketId) {
        Socket socket = chassis.socket(socketId);
        if (socket == null || socket.parent().isEmpty()) {
            return null;
        }
        Socket parentPartner = partner(chassis, socket.parent().get());
        for (Socket other : chassis.sockets()) {
            if (other.id().equals(socketId) || !other.accepts().equals(socket.accepts()) || other.parent().isEmpty()) {
                continue;
            }
            boolean mirrored = Math.abs(other.pivot().y - socket.pivot().y) < SAME && Math.abs(other.pivot().z - socket.pivot().z) < SAME;
            if (parentPartner != null) {
                // Off the partner parent: a limb's next joint has the same offset, a sideways one the opposite.
                boolean sameSide = Math.abs(other.pivot().x + socket.pivot().x) < SAME || Math.abs(other.pivot().x - socket.pivot().x) < SAME;
                if (other.parent().get().equals(parentPartner.id()) && mirrored && sameSide) {
                    return other;
                }
            } else if (other.parent().equals(socket.parent()) && mirrored && Math.abs(socket.pivot().x) > SAME
                    && Math.abs(other.pivot().x + socket.pivot().x) < SAME) {
                return other;
            }
        }
        return null;
    }
}
