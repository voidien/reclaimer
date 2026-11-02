package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.RelayBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;

/** Lua functions of the Relay (CC: Tweaked generic peripheral). Read-only: a relay does nothing but run. */
public final class RelayPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("relay").toString();
    }

    /** The relay's id, which it keeps for good. */
    @LuaFunction(mainThread = true)
    public final String getRelayId(RelayBlockEntity relay) {
        return relay.id().toString();
    }

    /** Whether it has power; a dark relay passes nothing on. */
    @LuaFunction(mainThread = true)
    public final boolean isOnline(RelayBlockEntity relay) {
        return relay.isLit();
    }

    /** Forge Energy in the buffer. */
    @LuaFunction(mainThread = true)
    public final int getEnergy(RelayBlockEntity relay) {
        return relay.storedEnergy();
    }

    /** The size of the buffer. */
    @LuaFunction(mainThread = true)
    public final int getEnergyCapacity(RelayBlockEntity relay) {
        return relay.energyCapacity();
    }

    /** Forge Energy it uses each tick while it runs. */
    @LuaFunction(mainThread = true)
    public final int getEnergyUse(RelayBlockEntity relay) {
        return relay.energyPerTick();
    }

    /** How many relays this dimension knows. */
    @LuaFunction(mainThread = true)
    public final int getRelayCount(RelayBlockEntity relay) {
        return relay.relaysInLevel();
    }
}
