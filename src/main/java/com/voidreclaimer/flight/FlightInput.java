package com.voidreclaimer.flight;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Which thruster keys are held, packed into a bit set. */
public record FlightInput(byte bits) {
    private static final int FORWARD = 1;
    private static final int BACK = 1 << 1;
    private static final int LEFT = 1 << 2;
    private static final int RIGHT = 1 << 3;
    private static final int UP = 1 << 4;
    private static final int DOWN = 1 << 5;
    /** A walking body (the Mech) has its thrusters engaged. Not a key: set by the client controller. */
    private static final int ENGAGED = 1 << 6;

    public static final FlightInput NONE = new FlightInput((byte) 0);
    public static final StreamCodec<ByteBuf, FlightInput> STREAM_CODEC =
            ByteBufCodecs.BYTE.map(FlightInput::new, FlightInput::bits);

    public static FlightInput of(boolean forward, boolean back, boolean left, boolean right, boolean up, boolean down) {
        int bits = (forward ? FORWARD : 0) | (back ? BACK : 0) | (left ? LEFT : 0)
                | (right ? RIGHT : 0) | (up ? UP : 0) | (down ? DOWN : 0);
        return new FlightInput((byte) bits);
    }

    /**
     * Whether a walking body that is flying should drop back to walking: it is on the ground and no up-thrust is held
     * (it engaged by holding Space and let go, or it landed).
     */
    public static boolean shouldDisengage(boolean onGround, boolean upHeld) {
        return onGround && !upHeld;
    }

    public boolean engaged() {
        return (bits & ENGAGED) != 0;
    }

    public FlightInput withEngaged(boolean value) {
        return new FlightInput((byte) (value ? bits | ENGAGED : bits & ~ENGAGED));
    }

    /** -1, 0 or 1 along the view direction. */
    public int forwardAxis() {
        return axis(FORWARD, BACK);
    }

    /** -1, 0 or 1 to the right of the view. */
    public int strafeAxis() {
        return axis(RIGHT, LEFT);
    }

    /** -1, 0 or 1 along the view's up direction. */
    public int verticalAxis() {
        return axis(UP, DOWN);
    }

    public boolean isThrusting() {
        return forwardAxis() != 0 || strafeAxis() != 0 || verticalAxis() != 0;
    }

    private int axis(int positive, int negative) {
        return ((bits & positive) != 0 ? 1 : 0) - ((bits & negative) != 0 ? 1 : 0);
    }
}
