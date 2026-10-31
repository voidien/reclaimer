package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.ship.HelmBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Lua functions of the Helm (CC: Tweaked generic peripheral). Read-only: a computer cannot take the stick. */
public final class HelmPeripheral implements GenericPeripheral {
    private static final String[] MOVES = {"forward", "back", "right", "left", "up", "down"};
    private static final String[] TURNS = {"rollRight", "rollLeft", "noseUp", "noseDown", "yawLeft", "yawRight"};

    @Override
    public String id() {
        return VoidReclaimer.id("helm").toString();
    }

    /** Whether the Helm is built into a ship. */
    @LuaFunction(mainThread = true)
    public final boolean isOnShip(HelmBlockEntity helm) {
        return helm.readout().onShip();
    }

    /** The pilot's name, or nil. */
    @Nullable
    @LuaFunction(mainThread = true)
    public final String getPilot(HelmBlockEntity helm) {
        return helm.pilot() == null ? null : helm.pilot().getGameProfile().getName();
    }

    /** Thrusters the Helm found on its ship. */
    @LuaFunction(mainThread = true)
    public final int getThrusterCount(HelmBlockEntity helm) {
        return helm.readout().thrusters();
    }

    /** How well the ship can push each way, 0 to 1: forward, back, right, left, up, down. */
    @LuaFunction(mainThread = true)
    public final Map<String, Double> getTranslation(HelmBlockEntity helm) {
        return named(MOVES, helm.readout().move());
    }

    /** How well the ship can turn each way, 0 to 1: rollRight, rollLeft, noseUp, noseDown, yawLeft, yawRight. */
    @LuaFunction(mainThread = true)
    public final Map<String, Double> getRotation(HelmBlockEntity helm) {
        return named(TURNS, helm.readout().turn());
    }

    /** Fuel and energy across the ship's thrusters: {fuel, fuelCapacity, energy, energyCapacity}. */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getSupplies(HelmBlockEntity helm) {
        HelmBlockEntity.Readout r = helm.readout();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fuel", r.fuel());
        out.put("fuelCapacity", r.fuelCapacity());
        out.put("energy", r.energy());
        out.put("energyCapacity", r.energyCapacity());
        return out;
    }

    /** The ship's speed along the Helm's axes, blocks per second: {@code surge} forward, {@code sway} right, {@code heave} up. */
    @LuaFunction(mainThread = true)
    public final Map<String, Double> getVelocity(HelmBlockEntity helm) {
        HelmBlockEntity.Readout r = helm.readout();
        Map<String, Double> out = named(new String[] {"surge", "sway", "heave"}, r.velocity());
        out.put("speed", r.speed());
        return out;
    }

    /** The ship's turning rate, degrees per second, as the pilot reads it: roll right, nose up, yaw right. */
    @LuaFunction(mainThread = true)
    public final Map<String, Double> getSpin(HelmBlockEntity helm) {
        return named(new String[] {"rollRight", "noseUp", "yawRight"}, helm.readout().spin());
    }

    /** Which way the ship points, degrees: nose above the horizon, roll to the right, compass heading of the nose. */
    @LuaFunction(mainThread = true)
    public final Map<String, Double> getAttitude(HelmBlockEntity helm) {
        var a = helm.readout().attitude();
        return named(new String[] {"pitch", "roll", "heading"}, new double[] {a.pitch(), a.roll(), a.heading()});
    }

    private static Map<String, Double> named(String[] names, double[] values) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            out.put(names[i], values[i]);
        }
        return out;
    }
}
