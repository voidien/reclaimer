package com.voidreclaimer.helper;

/**
 * The mining job's phases, pure: a drone flies to the nearest asteroid block near its mine point, digs it for a while,
 * carries what it broke to its destination, and comes back for the next; with no rock in reach or no room to unload it
 * waits a while and looks again. Without a mine point and a destination it idles.
 */
public final class Mining {
    public enum Phase { IDLE, TO_ROCK, DIGGING, TO_DESTINATION, WAITING }

    public enum Action { NONE, DIG, BREAK, UNLOAD }

    private Mining() {}

    /** What to do this tick: dig while beside the rock, break it when the dig is done, unload on arriving at the destination. */
    public static Action actionOn(Phase phase, boolean arrived, boolean digDone) {
        return switch (phase) {
            case DIGGING -> digDone ? Action.BREAK : Action.DIG;
            case TO_DESTINATION -> arrived ? Action.UNLOAD : Action.NONE;
            default -> Action.NONE;
        };
    }

    /**
     * The phase after this tick: {@code linked} is a mine point and a destination, {@code rockFound} whether a rock was
     * found to fly to (TO_ROCK) or is still there (DIGGING), {@code arrived} whether the drone reached its hover point,
     * {@code carrying} whether it holds anything now, {@code waitOver} whether a wait ran out.
     */
    public static Phase after(Phase phase, Action action, boolean linked, boolean rockFound, boolean arrived, boolean carrying, boolean waitOver) {
        if (!linked) {
            return Phase.IDLE;
        }
        return switch (phase) {
            case IDLE -> carrying ? Phase.TO_DESTINATION : (rockFound ? Phase.TO_ROCK : Phase.WAITING);
            case TO_ROCK -> !rockFound ? Phase.WAITING : (arrived ? Phase.DIGGING : Phase.TO_ROCK);
            case DIGGING -> !rockFound ? Phase.WAITING : (action == Action.BREAK ? (carrying ? Phase.TO_DESTINATION : Phase.WAITING) : Phase.DIGGING);
            case TO_DESTINATION -> action == Action.UNLOAD ? (carrying ? Phase.WAITING : (rockFound ? Phase.TO_ROCK : Phase.WAITING)) : Phase.TO_DESTINATION;
            case WAITING -> !waitOver ? Phase.WAITING : (carrying ? Phase.TO_DESTINATION : (rockFound ? Phase.TO_ROCK : Phase.WAITING));
        };
    }

    /** Whether the drone is flying somewhere in this phase (the thrust animation). */
    public static boolean moving(Phase phase) {
        return phase == Phase.TO_ROCK || phase == Phase.TO_DESTINATION;
    }
}
