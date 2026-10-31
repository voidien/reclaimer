package com.voidreclaimer.helper;

/**
 * The ferry job's phases, pure: a drone flies to its source, loads, flies to its destination, unloads, and goes back;
 * with nothing to take or no room to give it waits a while and tries again. Without both links it idles.
 */
public final class Ferry {
    public enum Phase { IDLE, TO_SOURCE, TO_DESTINATION, WAITING }

    public enum Action { NONE, LOAD, UNLOAD }

    private Ferry() {}

    /** What to do this tick: load on arriving at the source, unload on arriving at the destination. */
    public static Action actionOn(Phase phase, boolean arrived) {
        if (!arrived) {
            return Action.NONE;
        }
        return switch (phase) {
            case TO_SOURCE -> Action.LOAD;
            case TO_DESTINATION -> Action.UNLOAD;
            default -> Action.NONE;
        };
    }

    /**
     * The phase after this tick, given what was done ({@code action}), whether both links are set, whether the drone
     * carries anything now, and whether a wait has run out.
     */
    public static Phase after(Phase phase, Action action, boolean linked, boolean carrying, boolean waitOver) {
        if (!linked) {
            return Phase.IDLE;
        }
        return switch (phase) {
            case IDLE -> carrying ? Phase.TO_DESTINATION : Phase.TO_SOURCE;
            case TO_SOURCE -> action == Action.LOAD ? (carrying ? Phase.TO_DESTINATION : Phase.WAITING) : Phase.TO_SOURCE;
            case TO_DESTINATION -> action == Action.UNLOAD ? (carrying ? Phase.WAITING : Phase.TO_SOURCE) : Phase.TO_DESTINATION;
            case WAITING -> waitOver ? (carrying ? Phase.TO_DESTINATION : Phase.TO_SOURCE) : Phase.WAITING;
        };
    }

    /** Whether the drone is flying somewhere in this phase (the thrust animation). */
    public static boolean moving(Phase phase) {
        return phase == Phase.TO_SOURCE || phase == Phase.TO_DESTINATION;
    }
}
