package com.voidreclaimer.compat.cc;

import com.voidreclaimer.VoidReclaimer;
import java.util.List;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * CC: Tweaked integration (optional, like JEI). Every block of ours with state worth reading or an action worth taking
 * gets a generic peripheral here: a computer next to the block, or on a wired modem attached to it, can call its Lua
 * functions. Nothing in this package is loaded unless {@code computercraft} is installed: {@link #onCommonSetup} is the
 * only entry point and checks first, and the peripheral classes are only named inside {@link Sources}.
 */
public final class CCCompat {
    public static final String MOD_ID = "computercraft";

    private CCCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Peripherals of plain blocks (no block entity), attached through CC's block capability. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (isLoaded()) {
            Sources.registerCapabilities(event);
        }
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (isLoaded()) {
            event.enqueueWork(Sources::register);
        }
    }

    /** Kept apart so that the CC classes it names are only loaded when CC is present. */
    public static final class Sources {
        private Sources() {}

        /** Every generic source we register; tests check against it. */
        public static List<dan200.computercraft.api.lua.GenericSource> all() {
            return List.of(new MachinePeripheral(), new DysonPeripheral(), new SpirePeripheral(), new SpawnTravelPeripherals.BackupCore(), new ThrusterPeripheral(), new HelmPeripheral());
        }

        static void registerCapabilities(RegisterCapabilitiesEvent event) {
            var capability = dan200.computercraft.api.peripheral.PeripheralCapability.get();
            event.registerBlock(capability, (level, pos, state, entity, side) -> new SpawnTravelPeripherals.TetherAnchor(level, pos),
                    com.voidreclaimer.registry.ModBlocks.TETHER_ANCHOR.get());
            event.registerBlock(capability, (level, pos, state, entity, side) -> new SpawnTravelPeripherals.PortalFrame(level, pos),
                    com.voidreclaimer.registry.ModBlocks.PORTAL_FRAME.get());
        }

        static void register() {
            for (var source : all()) {
                dan200.computercraft.api.ComputerCraftAPI.registerGenericSource(source);
            }
            VoidReclaimer.LOGGER.info("CC: Tweaked found: registered {} peripheral sources", all().size());
        }
    }
}
