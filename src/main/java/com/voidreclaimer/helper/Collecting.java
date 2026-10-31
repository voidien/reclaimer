package com.voidreclaimer.helper;

/**
 * The dust-collecting job's phases, pure: a drone flies to its collect point, gathers cloud dust there into its tank,
 * and when the tank is full flies to its owner and hands the dust over; with the owner out of reach, or the owner's own
 * storage full, it waits a while and tries again. Without a collect point it idles.
 */
public final class Collecting {
    public enum Phase { IDLE, TO_POINT, GATHERING, TO_OWNER, WAITING }

    public enum Action { NONE, GATHER, DELIVER }

    private Collecting() {}

    /** What to do this tick: gather at the point, deliver on reaching the owner. */
    public static Action actionOn(Phase phase, boolean arrived) {
        return switch (phase) {
            case GATHERING -> Action.GATHER;
            case TO_OWNER -> arrived ? Action.DELIVER : Action.NONE;
            default -> Action.NONE;
        };
    }

    /**
     * The phase after this tick: {@code linked} is a collect point, {@code full} whether the tank has reached its
     * capacity, {@code holding} whether it holds any dust, {@code ownerNear} whether the owner is online and within
     * reach, {@code arrived} whether the drone reached its target, {@code waitOver} whether a wait ran out.
     */
    public static Phase after(Phase phase, Action action, boolean linked, boolean full, boolean holding, boolean ownerNear,
                              boolean arrived, boolean waitOver) {
        if (!linked) {
            return Phase.IDLE;
        }
        return switch (phase) {
            case IDLE -> full ? (ownerNear ? Phase.TO_OWNER : Phase.WAITING) : Phase.TO_POINT;
            case TO_POINT -> arrived ? Phase.GATHERING : Phase.TO_POINT;
            case GATHERING -> !full ? Phase.GATHERING : (ownerNear ? Phase.TO_OWNER : Phase.WAITING);
            case TO_OWNER -> !ownerNear ? Phase.WAITING : (action == Action.DELIVER ? (holding ? Phase.WAITING : Phase.TO_POINT) : Phase.TO_OWNER);
            case WAITING -> !waitOver ? Phase.WAITING : (full ? (ownerNear ? Phase.TO_OWNER : Phase.WAITING) : Phase.TO_POINT);
        };
    }

    /** Whether the drone is flying somewhere in this phase (the thrust animation). */
    public static boolean moving(Phase phase) {
        return phase == Phase.TO_POINT || phase == Phase.TO_OWNER;
    }
}
