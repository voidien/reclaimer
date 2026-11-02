package com.voidreclaimer.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.api.body.ModuleStats;
import org.junit.jupiter.api.Test;

class AfterburnerStateTest {
    private static final ModuleStats.Afterburner SPEC = new ModuleStats.Afterburner(1.5f, 5f, 20f, 16f);
    private static final int BURN = 100;
    private static final int COOL = 400;

    private static AfterburnerState run(AfterburnerState state, boolean wanted, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            state = state.tick(wanted, SPEC);
        }
        return state;
    }

    @Test
    void aBurnRaisesTheTopSpeedAtOnce() {
        AfterburnerState state = AfterburnerState.COLD.tick(true, SPEC);
        assertTrue(state.burning());
        assertEquals(1.5f, state.speedFactor());
        assertFalse(state.fading());
    }

    @Test
    void aFullBurnLocksUntilCold() {
        AfterburnerState spent = run(AfterburnerState.COLD, true, BURN + 2);
        assertTrue(spent.overheated(), "still burning after " + BURN + " ticks: " + spent);
        assertFalse(spent.burning());
        AfterburnerState nearly = run(spent, false, COOL - 20);
        assertTrue(nearly.overheated(), "cold too soon: " + nearly);
        assertFalse(nearly.tick(true, SPEC).burning(), "it burned while locked");
        AfterburnerState cold = run(nearly, false, 40);
        assertFalse(cold.overheated(), "never cooled: " + cold);
        assertTrue(cold.tick(true, SPEC).burning(), "a cold burner would not light");
    }

    @Test
    void aShortBurnNeedsOnlyAShortRest() {
        AfterburnerState burned = run(AfterburnerState.COLD, true, 20);
        assertEquals(0.2f, burned.heat(), 1e-4f);
        assertFalse(burned.overheated());
        assertTrue(run(burned, false, 40).tick(true, SPEC).burning(), "it would not relight part way through cooling");
        assertEquals(0f, run(burned, false, 81).heat(), 1e-4f);
    }

    @Test
    void theTopSpeedFadesOverASecondAfterABurn() {
        AfterburnerState burning = AfterburnerState.COLD.tick(true, SPEC);
        AfterburnerState fading = run(burning, false, 10);
        assertEquals(1.25f, fading.speedFactor(), 1e-4f);
        assertTrue(fading.fading());
        AfterburnerState done = run(fading, false, 10);
        assertEquals(1f, done.speedFactor(), 1e-4f);
        assertFalse(done.fading());
    }

    @Test
    void takingTheModuleOffLetsItFadeThenForgets() {
        AfterburnerState burning = run(AfterburnerState.COLD, true, 30);
        AfterburnerState state = burning.tick(false, null);
        assertTrue(state.fading());
        for (int tick = 0; tick < 20; tick++) {
            state = state.tick(false, null);
        }
        assertEquals(AfterburnerState.COLD, state);
    }
}
