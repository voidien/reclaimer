package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.UplinkSeatBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;

/** Lua functions of the Uplink Seat (CC: Tweaked generic peripheral). Read-only: a link needs a player to take it. */
public final class UplinkSeatPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("uplink_seat").toString();
    }

    /** The seat's id, which it keeps for good. */
    @LuaFunction(mainThread = true)
    public final String getSeatId(UplinkSeatBlockEntity seat) {
        return seat.id().toString();
    }

    /** Whether a player is linked from this seat. */
    @LuaFunction(mainThread = true)
    public final boolean isOccupied(UplinkSeatBlockEntity seat) {
        return seat.occupant().isPresent();
    }

    /** That player's name, or an empty string. */
    @LuaFunction(mainThread = true)
    public final String getOccupant(UplinkSeatBlockEntity seat) {
        return seat.occupant().map(player -> player.getScoreboardName()).orElse("");
    }
}
