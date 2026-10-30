package com.voidreclaimer.ship;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The pilot's six axes, each -1, 0 or 1, packed into a bit set: surge (W/S), sway (A/D), heave (Space/Shift), roll
 * (Q/E), pitch (arrow up/down) and yaw (arrow left/right). Positive is forward, right, up, roll right, nose up, turn
 * left (the right-hand rule about the ship's up axis).
 */
public record PilotInput(short bits) {
    private static final int FORWARD = 1, BACK = 1 << 1, RIGHT = 1 << 2, LEFT = 1 << 3, UP = 1 << 4, DOWN = 1 << 5,
            ROLL_RIGHT = 1 << 6, ROLL_LEFT = 1 << 7, NOSE_UP = 1 << 8, NOSE_DOWN = 1 << 9, YAW_LEFT = 1 << 10, YAW_RIGHT = 1 << 11;

    public static final PilotInput NONE = new PilotInput((short) 0);
    public static final StreamCodec<ByteBuf, PilotInput> STREAM_CODEC = ByteBufCodecs.SHORT.map(PilotInput::new, PilotInput::bits);

    public static PilotInput of(boolean forward, boolean back, boolean right, boolean left, boolean up, boolean down,
                                boolean rollRight, boolean rollLeft, boolean noseUp, boolean noseDown, boolean yawLeft, boolean yawRight) {
        int bits = (forward ? FORWARD : 0) | (back ? BACK : 0) | (right ? RIGHT : 0) | (left ? LEFT : 0) | (up ? UP : 0) | (down ? DOWN : 0)
                | (rollRight ? ROLL_RIGHT : 0) | (rollLeft ? ROLL_LEFT : 0) | (noseUp ? NOSE_UP : 0) | (noseDown ? NOSE_DOWN : 0)
                | (yawLeft ? YAW_LEFT : 0) | (yawRight ? YAW_RIGHT : 0);
        return new PilotInput((short) bits);
    }

    public int surge() {
        return axis(FORWARD, BACK);
    }

    public int sway() {
        return axis(RIGHT, LEFT);
    }

    public int heave() {
        return axis(UP, DOWN);
    }

    public int roll() {
        return axis(ROLL_RIGHT, ROLL_LEFT);
    }

    public int pitch() {
        return axis(NOSE_UP, NOSE_DOWN);
    }

    public int yaw() {
        return axis(YAW_LEFT, YAW_RIGHT);
    }

    public boolean isActive() {
        return bits != 0;
    }

    private int axis(int positive, int negative) {
        return ((bits & positive) != 0 ? 1 : 0) - ((bits & negative) != 0 ? 1 : 0);
    }
}
