package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HostRoleTest {
    @Test
    void everyRoleSurvivesASave() {
        for (HostRole role : HostRole.values()) {
            assertEquals(role, HostRole.parse(role.id()));
        }
    }

    @Test
    void onlyAMirrorIsAwake() {
        assertTrue(HostRole.SHELL.dormant());
        assertTrue(HostRole.WRECK.dormant());
        assertFalse(HostRole.MIRROR.dormant());
    }

    @Test
    void anUnknownRoleIsAShellSoTheBodyIsNotLost() {
        assertEquals(HostRole.SHELL, HostRole.parse(""));
        assertEquals(HostRole.SHELL, HostRole.parse("from_a_newer_version"));
    }
}
