package com.voidreclaimer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** A ship's hit is about to break a block, on the game bus and the server side, once for each block. Cancel it and the block stays. */
public final class ShipCrashEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos pos;
    private final double speed;

    public ShipCrashEvent(ServerLevel level, BlockPos pos, double speed) {
        this.level = level;
        this.pos = pos;
        this.speed = speed;
    }

    public ServerLevel getLevel() {
        return level;
    }

    /** The block, on a ship in the ship's own coordinates. */
    public BlockPos getPos() {
        return pos;
    }

    /** How fast it was hit, in blocks a second, along what hit it. */
    public double getSpeed() {
        return speed;
    }
}
