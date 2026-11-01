package com.voidreclaimer.gametest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTestFilterTest {
    @Test
    void matchesClassOrMethodIgnoringCase() {
        assertTrue(GameTestFilter.matches("zerogmining", "ZeroGMiningGameTests", "handMining"));
        assertTrue(GameTestFilter.matches("HANDMINING", "ZeroGMiningGameTests", "handMining"));
        assertFalse(GameTestFilter.matches("flight", "ZeroGMiningGameTests", "handMining"));
    }

    @Test
    void commaSeparatedTermsAreAlternatives() {
        assertTrue(GameTestFilter.matches("flight, zerog", "ZeroGMiningGameTests", "handMining"));
        assertFalse(GameTestFilter.matches(" , ", "ZeroGMiningGameTests", "handMining"));
    }

    @Test
    void chunkSavesAreSkippedOnlyWhenAskedAndNeverAcrossARestart() {
        assertTrue(GameTestFilter.skipsChunkSaves(true, null), "a full run");
        assertTrue(GameTestFilter.skipsChunkSaves(true, "zerogmining"), "a filtered run");
        assertFalse(GameTestFilter.skipsChunkSaves(false, null), "not asked");
        assertFalse(GameTestFilter.skipsChunkSaves(true, "aCrashKeepsOnlyWhatWasSavedBeforeRestart"), "first half of a restart test");
        assertFalse(GameTestFilter.skipsChunkSaves(true, "aLinkOutlivesACrashAfterRestart"), "second half");
    }

    @Test
    void chunksAreReleasedExceptAcrossARestart() {
        assertTrue(GameTestFilter.releasesChunks(null), "a full run");
        assertTrue(GameTestFilter.releasesChunks("LinkLedger"), "a filtered run");
        assertFalse(GameTestFilter.releasesChunks("aLinkOutlivesACrashBeforeRestart"), "first half of a restart test");
        assertFalse(GameTestFilter.releasesChunks("aLinkOutlivesACrashAfterRestart"), "second half");
    }

    @Test
    void aTestThatSpansARestartIsNeverInANormalRun() {
        assertFalse(GameTestFilter.registers(null, "RestartGameTests", "aCrashKeepsOnlyWhatWasSavedBeforeRestart"));
        assertFalse(GameTestFilter.registers("", "RestartGameTests", "aCrashKeepsOnlyWhatWasSavedAfterRestart"));
        assertFalse(GameTestFilter.registers("RestartGameTests", "RestartGameTests", "aCrashKeepsOnlyWhatWasSavedBeforeRestart"), "a filter on its class");
        assertFalse(GameTestFilter.registers("aCrash", "RestartGameTests", "aCrashKeepsOnlyWhatWasSavedBeforeRestart"), "part of its name");
    }

    @Test
    void aTestThatSpansARestartRunsWhenItsPhaseIsNamed() {
        assertTrue(GameTestFilter.registers("aCrashKeepsOnlyWhatWasSavedBeforeRestart", "RestartGameTests", "aCrashKeepsOnlyWhatWasSavedBeforeRestart"));
        assertFalse(GameTestFilter.registers("aCrashKeepsOnlyWhatWasSavedBeforeRestart", "RestartGameTests", "aCrashKeepsOnlyWhatWasSavedAfterRestart"), "not the other phase");
    }

    @Test
    void otherTestsAreFilteredAsBefore() {
        assertTrue(GameTestFilter.registers(null, "ThrusterGameTests", "thrustPushes"));
        assertTrue(GameTestFilter.registers("", "ThrusterGameTests", "thrustPushes"));
        assertTrue(GameTestFilter.registers("thruster", "ThrusterGameTests", "thrustPushes"));
        assertFalse(GameTestFilter.registers("dust", "ThrusterGameTests", "thrustPushes"));
    }
}
