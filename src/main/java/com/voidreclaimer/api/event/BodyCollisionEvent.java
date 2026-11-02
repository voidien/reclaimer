package com.voidreclaimer.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

/**
 * A body hit something in flight, on the game bus: a big body with its hit boxes, or a smaller one against a big body. Flight runs on the
 * client that flies the body, so this fires there. One crash is one event, and the size of {@link #lost()} is how hard it was.
 */
public final class BodyCollisionEvent extends Event {
    private final Player player;
    private final Vec3 wanted;
    private final Vec3 kept;

    public BodyCollisionEvent(Player player, Vec3 wanted, Vec3 kept) {
        this.player = player;
        this.wanted = wanted;
        this.kept = kept;
    }

    public Player getPlayer() {
        return player;
    }

    /** The speed it hit at, in blocks a tick. */
    public Vec3 wanted() {
        return wanted;
    }

    /** The speed it has left, along whatever it hit. */
    public Vec3 kept() {
        return kept;
    }

    /** The part of its speed that went into what it hit: its direction is the contact's, and its length the speed of the hit. */
    public Vec3 lost() {
        return wanted.subtract(kept);
    }
}
