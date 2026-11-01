package com.voidreclaimer.compat.cc;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.earth.Sectors;
import com.voidreclaimer.terraform.SpireAssembly;
import com.voidreclaimer.terraform.SpireBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

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

    /** Blocks from the tower to the far edge of its sector; 0 outside the planet. */
    @LuaFunction(mainThread = true)
    public final int getRadius(SpireBlockEntity spire) {
        return spire.radius();
    }

    /** Whether the tower is charging for a pulse. */
    @LuaFunction(mainThread = true)
    public final boolean isCharging(SpireBlockEntity spire) {
        return spire.charging();
    }

    /** How far the charge has come, 0 to 1 (0 when none is under way). */
    @LuaFunction(mainThread = true)
    public final double getCharge(SpireBlockEntity spire) {
        return spire.chargeProgress();
    }

    /** What one pulse burns: {@code catalysts}, and {@code water} and {@code slurry} in millibuckets. */
    @LuaFunction(mainThread = true)
    public final Map<String, Integer> getCost(SpireBlockEntity spire) {
        return Map.of("catalysts", Config.SPIRE_CATALYSTS.get(), "water", Config.SPIRE_WATER.get(), "slurry", Config.SPIRE_SLURRY.get());
    }

    /** What is loaded: {@code catalysts}, and {@code water} and {@code slurry} in millibuckets. */
    @LuaFunction(mainThread = true)
    public final Map<String, Integer> getStock(SpireBlockEntity spire) {
        return Map.of("catalysts", spire.catalyst().getStackInSlot(0).getCount(), "water", spire.waterTank().getFluidAmount(),
                "slurry", spire.slurryTank().getFluidAmount());
    }

    /** The sector this tower stands in as a table of {@code column}, {@code row} (1 to 4) and {@code restored}; nil off the planet. */
    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, Object> getSector(SpireBlockEntity spire) {
        int sector = spire.sector();
        if (sector == Sectors.NONE) {
            return null;
        }
        SectorMap map = EarthRestoration.map(spire.getLevel().getServer());
        return Map.of("column", Sectors.column(sector) + 1, "row", Sectors.row(sector) + 1, "restored", map.isRestored(sector));
    }

    /** Whether each sector is restored: a list of 16 booleans, row by row from the north-west. Reads the planet from any dimension. */
    @LuaFunction(mainThread = true)
    public final Map<Integer, Boolean> getSectors(SpireBlockEntity spire) {
        SectorMap map = EarthRestoration.map(spire.getLevel().getServer());
        Map<Integer, Boolean> sectors = new HashMap<>();
        for (int sector = 0; sector < Sectors.COUNT; sector++) {
            sectors.put(sector + 1, map.isRestored(sector));
        }
        return sectors;
    }

    /** How many sectors of the planet are restored, 0 to 16. */
    @LuaFunction(mainThread = true)
    public final int getRestoredCount(SpireBlockEntity spire) {
        return EarthRestoration.map(spire.getLevel().getServer()).count();
    }

    /**
     * Charges and fires the Genesis Pulse, as a right-click would. Returns {@code true}, or {@code false} and why:
     * "busy", "wrong_world" (only on ruined Earth), "not_assembled", "off_planet", "sector_restored", "no_profile",
     * "no_catalyst", "no_water", "no_slurry" or "no_rotation".
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
