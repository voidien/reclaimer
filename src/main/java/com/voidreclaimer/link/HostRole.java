package com.voidreclaimer.link;

import java.util.Locale;

/** What a {@link BodyHost} is for. A fixed set: each role is a rule of remote control, not a kind of content. */
public enum HostRole {
    /** A built body in the world, inert until the player links to it. */
    SHELL,
    /** What is left of a destroyed shell: it only holds the contents, until they are taken out. */
    WRECK,
    /** The display copy of the player's main body at the seat. Never saved: the seat rebuilds it. */
    MIRROR;

    /** The name in a save. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The role saved as {@code id}; anything unknown is a shell, so a body in a save from a newer version is not lost. */
    public static HostRole parse(String id) {
        for (HostRole role : values()) {
            if (role.id().equals(id)) {
                return role;
            }
        }
        return SHELL;
    }
}
