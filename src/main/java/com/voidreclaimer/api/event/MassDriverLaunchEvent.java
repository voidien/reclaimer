package com.voidreclaimer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on the game bus (server side) when a Kinetic Mass Driver fires one payload, before the Dyson Sphere counters change.
 * The counts start as what the payload's item tag says (one satellite or one truss module). Change them, or cancel to launch
 * nothing: a cancelled launch still uses up the payload.
 */
public class MassDriverLaunchEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos driverPos;
    private final ItemStack payload;
    private long satellites;
    private long trusses;

    public MassDriverLaunchEvent(ServerLevel level, BlockPos driverPos, ItemStack payload, long satellites, long trusses) {
        this.level = level;
        this.driverPos = driverPos;
        this.payload = payload;
        this.satellites = satellites;
        this.trusses = trusses;
    }

    public ServerLevel getLevel() {
        return level;
    }

    /** Position of the Kinetic Drive Base. */
    public BlockPos getDriverPos() {
        return driverPos;
    }

    /** A copy of the launched item (one payload). */
    public ItemStack getPayload() {
        return payload;
    }

    public long getSatellites() {
        return satellites;
    }

    public void setSatellites(long satellites) {
        this.satellites = Math.max(0L, satellites);
    }

    public long getTrusses() {
        return trusses;
    }

    public void setTrusses(long trusses) {
        this.trusses = Math.max(0L, trusses);
    }
}
