package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.ship.ChemicalThrusterBlockEntity;
import com.voidreclaimer.ship.ElectricThrusterBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.List;
import java.util.Map;

/**
 * Lua functions of the ship thrusters (CC: Tweaked generic peripheral). Read-only: a computer sets the throttle the way a
 * player does, with redstone (a computer's own redstone output works).
 */
public final class ThrusterPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("thrusters").toString();
    }

    // ---- Ion and RCS Thrusters --------------------------------------------------------------------------------------

    /** Redstone throttle, 0 to 1. */
    @LuaFunction(mainThread = true)
    public final double getThrottle(ElectricThrusterBlockEntity thruster) {
        return thruster.throttle();
    }

    /** The force it pushes with right now. */
    @LuaFunction(mainThread = true)
    public final double getForce(ElectricThrusterBlockEntity thruster) {
        return thruster.force();
    }

    @LuaFunction(mainThread = true)
    public final boolean isFiring(ElectricThrusterBlockEntity thruster) {
        return thruster.force() > 0.0;
    }

    /** Forge Energy in the buffer. */
    @LuaFunction(mainThread = true)
    public final int getEnergy(ElectricThrusterBlockEntity thruster) {
        return thruster.storedEnergy();
    }

    @LuaFunction(mainThread = true)
    public final int getEnergyCapacity(ElectricThrusterBlockEntity thruster) {
        return thruster.energy().getMaxEnergyStored();
    }

    // ---- Chemical Thruster ------------------------------------------------------------------------------------------

    /** Redstone throttle, 0 to 1. */
    @LuaFunction(mainThread = true)
    public final double getThrottle(ChemicalThrusterBlockEntity thruster) {
        return thruster.throttle();
    }

    /** The force it pushes with right now. */
    @LuaFunction(mainThread = true)
    public final double getForce(ChemicalThrusterBlockEntity thruster) {
        return thruster.force();
    }

    @LuaFunction(mainThread = true)
    public final boolean isFiring(ChemicalThrusterBlockEntity thruster) {
        return thruster.force() > 0.0;
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed(ChemicalThrusterBlockEntity thruster) {
        return thruster.getSpeed();
    }

    /** The fuel tank: one {name, amount, capacity} in mB. */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getTanks(ChemicalThrusterBlockEntity thruster) {
        return LuaValues.tanks(thruster.tank());
    }
}
