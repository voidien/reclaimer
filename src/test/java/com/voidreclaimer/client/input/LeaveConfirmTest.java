package com.voidreclaimer.client.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeaveConfirmTest {
    @Test
    void theSecondPressLeavesAndTheFirstDoesNot() {
        LeaveConfirm confirm = new LeaveConfirm();
        assertFalse(confirm.press(100), "one press must not leave");
        assertTrue(confirm.press(100 + LeaveConfirm.WINDOW_TICKS), "a second press in time leaves");
    }

    @Test
    void aSlowSecondPressStartsAgain() {
        LeaveConfirm confirm = new LeaveConfirm();
        confirm.press(100);
        assertFalse(confirm.press(101 + LeaveConfirm.WINDOW_TICKS), "too late: this counts as a first press");
        assertTrue(confirm.press(102 + LeaveConfirm.WINDOW_TICKS), "and this as its second");
    }

    @Test
    void leavingLeavesNothingArmed() {
        LeaveConfirm confirm = new LeaveConfirm();
        confirm.press(100);
        confirm.press(101);
        assertFalse(confirm.press(102), "a third press is a new first press");
    }
}
