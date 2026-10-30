package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.machine.CloudSiphonBlockEntity;
import com.voidreclaimer.machine.ElectrolyzerBlockEntity;
import com.voidreclaimer.machine.SpinnerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.List;
import java.util.Map;

/**
 * Lua functions of the factory machines (CC: Tweaked generic peripheral). Read-only: the machines have no actions a
 * player takes by hand either. Speeds are in RPM as Create reports them; progress is 0 to 1. See {@code docs/CC_PERIPHERALS.md}.
 */
public final class MachinePeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("machines").toString();
    }

    // ---- Cloud Siphon ---------------------------------------------------------------------------------------------

    /** Cloud density at the siphon as of its last sample (0 outside a cloud). */
    @LuaFunction(mainThread = true)
    public final double getDensity(CloudSiphonBlockEntity siphon) {
        return siphon.density();
    }

    /** Power from the shaft (and any addon), 1 at 64 RPM; electricity tops it up while working. */
    @LuaFunction(mainThread = true)
    public final double getPower(CloudSiphonBlockEntity siphon) {
        return siphon.power();
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed(CloudSiphonBlockEntity siphon) {
        return siphon.getSpeed();
    }

    /** Forge Energy in the buffer. */
    @LuaFunction(mainThread = true)
    public final int getEnergy(CloudSiphonBlockEntity siphon) {
        return siphon.storedEnergy();
    }

    @LuaFunction(mainThread = true)
    public final int getEnergyCapacity(CloudSiphonBlockEntity siphon) {
        return siphon.energy().getMaxEnergyStored();
    }

    /** The output slots: slot number to {name, count}. */
    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, Object>> getItems(CloudSiphonBlockEntity siphon) {
        return LuaValues.items(siphon.inventory());
    }

    // ---- Electrolyzer ---------------------------------------------------------------------------------------------

    @LuaFunction(mainThread = true)
    public final double getProgress(ElectrolyzerBlockEntity electrolyzer) {
        return electrolyzer.progress();
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed(ElectrolyzerBlockEntity electrolyzer) {
        return electrolyzer.getSpeed();
    }

    /** Water, oxygen and hydrogen, in that order: {name, amount, capacity} in mB. */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getTanks(ElectrolyzerBlockEntity electrolyzer) {
        return LuaValues.tanks(electrolyzer.waterTank(), electrolyzer.oxygenTank(), electrolyzer.hydrogenTank());
    }

    // ---- Spinner --------------------------------------------------------------------------------------------------

    @LuaFunction(mainThread = true)
    public final double getProgress(SpinnerBlockEntity spinner) {
        return spinner.progress();
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed(SpinnerBlockEntity spinner) {
        return spinner.getSpeed();
    }

    /** Input and output slots: slot number to {name, count}. */
    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, Object>> getItems(SpinnerBlockEntity spinner) {
        return LuaValues.items(spinner.inventory());
    }
}
