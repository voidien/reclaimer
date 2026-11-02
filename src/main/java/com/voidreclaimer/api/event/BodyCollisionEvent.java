package com.voidreclaimer.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

/**
 * A big body's hit boxes stopped some of its move this tick, on the game bus. Flight runs on the client that flies the body, so this
 * fires there. For reacting to a crash, the size of {@link #lost()} is how hard it was.
 */
public final class BodyCollisionEvent extends Event {
    private final Player player;
    private final Vec3 wanted;
    private final Vec3 allowed;

    public BodyCollisionEvent(Player player, Vec3 wanted, Vec3 allowed) {
        this.player = player;
        this.wanted = wanted;
        this.allowed = allowed;
    }

    public Player getPlayer() {
        return player;
    }

    /** The move the body tried to make, in blocks a tick. */
    public Vec3 wanted() {
        return wanted;
    }

    /** The move its boxes let it make. */
    public Vec3 allowed() {
        return allowed;
    }

    /** The part of the move that hit something: its direction is the contact's, and its length the speed of the hit. */
    public Vec3 lost() {
        return wanted.subtract(allowed);
    }
}
