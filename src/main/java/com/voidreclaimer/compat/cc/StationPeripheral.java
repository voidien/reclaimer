package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.station.StationAssembly;
import com.voidreclaimer.station.StationCoreBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lua functions of the Station Core (CC: Tweaked generic peripheral): what a player can do at the core by hand. */
public final class StationPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("station_core").toString();
    }

    /** The station's name. */
    @LuaFunction(mainThread = true)
    public final String getName(StationCoreBlockEntity core) {
        return core.stationName();
    }

    /** Renames the station (an empty name restores the default). */
    @LuaFunction(mainThread = true)
    public final void setName(StationCoreBlockEntity core, String name) {
        core.setStationName(name.length() > 32 ? name.substring(0, 32) : name);
    }

    /** Whether the core is built into an assembled station (a Sable ship) rather than sitting in the world. */
    @LuaFunction(mainThread = true)
    public final boolean isAssembled(StationCoreBlockEntity core) {
        return core.ship() != null;
    }

    /** Assembles the blocks connected to the core into a station, as sneak-use does. Returns what happened. */
    @LuaFunction(mainThread = true)
    public final String assemble(StationCoreBlockEntity core) {
        return StationAssembly.assemble(core).name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Disassembles the station back into the world, as sneak-use does. Returns what happened. */
    @LuaFunction(mainThread = true)
    public final String disassemble(StationCoreBlockEntity core) {
        return StationAssembly.disassemble(core).name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Where the core is in the world right now (a station moves). */
    @LuaFunction(mainThread = true)
    public final Map<String, Double> getPosition(StationCoreBlockEntity core) {
        Map<String, Double> out = new LinkedHashMap<>();
        var at = core.worldSpot();
        out.put("x", at.x);
        out.put("y", at.y);
        out.put("z", at.z);
        return out;
    }

    /** How many of the station chest's {@value StationCoreBlockEntity#SLOTS} slots hold something. */
    @LuaFunction(mainThread = true)
    public final int getFilledSlots(StationCoreBlockEntity core) {
        return core.filledSlots();
    }
}
