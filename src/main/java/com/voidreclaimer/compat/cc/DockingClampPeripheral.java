package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.station.DockingClampBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.Locale;

/** Lua functions of the Docking Clamp (CC: Tweaked generic peripheral): what sneak-use and redstone do. */
public final class DockingClampPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("docking_clamp").toString();
    }

    /** Whether a ship is docked. */
    @LuaFunction(mainThread = true)
    public final boolean isDocked(DockingClampBlockEntity clamp) {
        return clamp.isDocked();
    }

    /** The docked ship's name, or an empty string. */
    @LuaFunction(mainThread = true)
    public final String getDockedName(DockingClampBlockEntity clamp) {
        return clamp.dockedName();
    }

    /** What the clamp last did or found: idle, not_on_ship, no_visitor, misaligned, blocked, docked, released, empty or failed. */
    @LuaFunction(mainThread = true)
    public final String getStatus(DockingClampBlockEntity clamp) {
        return clamp.status().name().toLowerCase(Locale.ROOT);
    }

    /** Docks the ship touching the clamp's face; returns the status. */
    @LuaFunction(mainThread = true)
    public final String dock(DockingClampBlockEntity clamp) {
        return clamp.dock().name().toLowerCase(Locale.ROOT);
    }

    /** Lets the docked ship go; returns the status. */
    @LuaFunction(mainThread = true)
    public final String release(DockingClampBlockEntity clamp) {
        return clamp.release().name().toLowerCase(Locale.ROOT);
    }
}
