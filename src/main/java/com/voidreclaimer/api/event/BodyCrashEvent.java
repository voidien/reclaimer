package com.voidreclaimer.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * A body flew into something hard enough to matter, on the game bus and the server side, before any harm is done. Cancel it and nothing
 * happens; change {@link #setDamage the damage} or {@link #setBreaksBlocks whether blocks break} for a part that softens a hit.
 */
public final class BodyCrashEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final double speed;
    private float damage;
    private boolean breaksBlocks;

    public BodyCrashEvent(ServerPlayer player, double speed, float damage, boolean breaksBlocks) {
        this.player = player;
        this.speed = speed;
        this.damage = damage;
        this.breaksBlocks = breaksBlocks;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /** How fast it hit, in blocks a second, along what it hit. */
    public double getSpeed() {
        return speed;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0f, damage);
    }

    public boolean breaksBlocks() {
        return breaksBlocks;
    }

    public void setBreaksBlocks(boolean breaksBlocks) {
        this.breaksBlocks = breaksBlocks;
    }
}
