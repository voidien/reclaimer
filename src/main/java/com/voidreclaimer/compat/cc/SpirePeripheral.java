package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.terraform.SpireBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.Locale;

/**
 * Lua functions of the Terraforming Spire (CC: Tweaked generic peripheral). {@code pulse()} is the player's right-click
 * or a redstone pulse: the same {@link SpireBlockEntity#tryStart()}, so the same rules (ruined Earth only, one pulse at
 * a time).
 */
public final class SpirePeripheral implements GenericPeripheral {
    @Override
    public String id() {
        return VoidReclaimer.id("terraforming_spire").toString();
    }

    @LuaFunction(mainThread = true)
    public final boolean isRunning(SpireBlockEntity spire) {
        return spire.running();
    }

    /** How far the running pulse has swept, 0 to 1 (0 when idle). */
    @LuaFunction(mainThread = true)
    public final double getProgress(SpireBlockEntity spire) {
        return spire.progress();
    }

    /** Pulses finished at this Spire. */
    @LuaFunction(mainThread = true)
    public final int getPulses(SpireBlockEntity spire) {
        return spire.pulses();
    }

    /** Blocks from the Spire to the edge of its square area. */
    @LuaFunction(mainThread = true)
    public final int getRadius(SpireBlockEntity spire) {
        return spire.radius();
    }

    /**
     * Fires the Genesis Pulse, as a right-click would. Returns {@code true}, or {@code false} and why:
     * "busy", "wrong_world" (only on ruined Earth) or "no_profile".
     */
    @LuaFunction(mainThread = true)
    public final MethodResult pulse(SpireBlockEntity spire) {
        SpireBlockEntity.Start result = spire.tryStart();
        if (result == SpireBlockEntity.Start.STARTED) {
            return MethodResult.of(true);
        }
        return MethodResult.of(false, result.name().toLowerCase(Locale.ROOT));
    }
}
