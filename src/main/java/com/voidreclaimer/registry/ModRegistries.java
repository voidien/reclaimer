package com.voidreclaimer.registry;

import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.dust.DustKind;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Creates the mod's own registries (keys in {@link VoidReclaimerRegistries}). */
public final class ModRegistries {
    private ModRegistries() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModRegistries::onNewDatapackRegistries);
    }

    private static void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // Synced: the client needs them for the HUD, cloud generation and screens.
        event.dataPackRegistry(VoidReclaimerRegistries.DUST_KIND, DustKind.CODEC, DustKind.CODEC);
    }
}
