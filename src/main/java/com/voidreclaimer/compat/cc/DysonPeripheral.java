package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.machine.MassDriver;
import com.voidreclaimer.machine.MassDriverBaseBlockEntity;
import com.voidreclaimer.machine.PayloadChamberBlockEntity;
import com.voidreclaimer.machine.QuantumReceiverBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Lua functions of the Dyson launch chain (CC: Tweaked generic peripheral): the Kinetic Drive Base (the Mass Driver's
 * controller), the Payload Chamber and the Sub-Light Quantum Receiver, plus the sphere's progress from either end.
 * Read-only: the driver fires on its own once wound up, and a player has no way to make it launch either.
 */
public final class DysonPeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("dyson").toString();
    }

    /** The sphere as the server sees it: launch counters, sky tier, shell progress and remaining sunlight. */
    static Map<String, Object> sphere(BlockEntity entity) {
        DysonSphereData data = DysonSphereData.of(entity.getLevel());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("satellites", data.satellites());
        out.put("trusses", data.trusses());
        out.put("shellTotal", data.shellTotal());
        out.put("tier", data.tier());
        out.put("shellProgress", data.shellProgress());
        out.put("sunlight", data.sunlight());
        return out;
    }

    // ---- Kinetic Drive Base (Mass Driver) ---------------------------------------------------------------------------

    /** Whether the Payload Chamber and Barrel Nozzle sit on top of this base. */
    @LuaFunction(mainThread = true)
    public final boolean isAssembled(MassDriverBaseBlockEntity base) {
        return MassDriver.isAssembled(base.getLevel(), base.getBlockPos());
    }

    /** Wind-up of the next launch, 0 to 1. */
    @LuaFunction(mainThread = true)
    public final double getProgress(MassDriverBaseBlockEntity base) {
        return base.progress();
    }

    /** Payloads this driver has fired. */
    @LuaFunction(mainThread = true)
    public final int getLaunches(MassDriverBaseBlockEntity base) {
        return base.launches();
    }

    @LuaFunction(mainThread = true)
    public final double getSpeed(MassDriverBaseBlockEntity base) {
        return base.getSpeed();
    }

    /** Whether the chamber above holds a payload ready to fire. */
    @LuaFunction(mainThread = true)
    public final boolean hasPayload(MassDriverBaseBlockEntity base) {
        return base.getLevel().getBlockEntity(MassDriver.chamberPos(base.getBlockPos())) instanceof PayloadChamberBlockEntity chamber
                && chamber.firstPayloadSlot() >= 0;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getSphere(MassDriverBaseBlockEntity base) {
        return sphere(base);
    }

    // ---- Payload Chamber --------------------------------------------------------------------------------------------

    /** The chamber's slots: slot number to {name, count}. */
    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, Object>> getItems(PayloadChamberBlockEntity chamber) {
        return LuaValues.items(chamber.inventory());
    }

    // ---- Sub-Light Quantum Receiver ---------------------------------------------------------------------------------

    /** The speed it turns the network at (RPM), 0 before any truss is launched. */
    @LuaFunction(mainThread = true)
    public final double getSpeed(QuantumReceiverBlockEntity receiver) {
        return receiver.getGeneratedSpeed();
    }

    /** Stress capacity it adds, in SU (capacity per RPM times its speed). */
    @LuaFunction(mainThread = true)
    public final double getStressCapacity(QuantumReceiverBlockEntity receiver) {
        return receiver.calculateAddedStressCapacity() * receiver.getGeneratedSpeed();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getSphere(QuantumReceiverBlockEntity receiver) {
        return sphere(receiver);
    }
}
