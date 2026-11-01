package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.terraform.SpireAssembly;
import com.voidreclaimer.terraform.SpireBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.Locale;

/**
 * Lua functions of the Terraforming Spire (CC: Tweaked generic peripheral). {@code pulse()} is the player's right-click
 * or a redstone pulse: the same {@link SpireBlockEntity#tryStart()}, so the same rules (ruined Earth only, a standing
 * tower, one pulse at a time). {@code assemble()} and {@code disassemble()} are the player's sneak-use.
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

    /** Whether the frame is complete and the column filled. */
    @LuaFunction(mainThread = true)
    public final boolean isAssembled(SpireBlockEntity spire) {
        return spire.assembled();
    }

    /** Assembles the tower: {@code assembled}, {@code already_assembled}, {@code frame_incomplete}, {@code blocked} or {@code failed}. */
    @LuaFunction(mainThread = true)
    public final String assemble(SpireBlockEntity spire) {
        return SpireAssembly.assemble(spire).name().toLowerCase(Locale.ROOT);
    }

    /** Sets the tower down: {@code disassembled}, {@code not_assembled} or {@code failed}. */
    @LuaFunction(mainThread = true)
    public final String disassemble(SpireBlockEntity spire) {
        return SpireAssembly.disassemble(spire).name().toLowerCase(Locale.ROOT);
    }

    /** Shaft speed in RPM. */
    @LuaFunction(mainThread = true)
    public final double getSpeed(SpireBlockEntity spire) {
        return spire.getSpeed();
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
     * "busy", "wrong_world" (only on ruined Earth), "not_assembled" or "no_profile".
     */
    @LuaFunction(mainThread = true)
    public final MethodResult pulse(SpireBlockEntity spire) {
        SpireBlockEntity.Start result = spire.tryStart();
        if (result == SpireBlockEntity.Start.STARTED) {
            return MethodResult.of(true);
        }
        return MethodResult.of(false, result.key());
    }
}
