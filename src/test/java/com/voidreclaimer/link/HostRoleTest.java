package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HostRoleTest {
    @Test
    void everyRoleSurvivesASave() {
        for (HostRole role : HostRole.values()) {
            assertEquals(role, HostRole.parse(role.id()));
        }
    }

    @Test
    void anUnknownRoleIsAShellSoTheBodyIsNotLost() {
        assertEquals(HostRole.SHELL, HostRole.parse(""));
        assertEquals(HostRole.SHELL, HostRole.parse("from_a_newer_version"));
    }
}
