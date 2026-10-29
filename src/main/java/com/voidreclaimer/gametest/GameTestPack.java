package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/**
 * Adds {@code resources/gametest_pack} on the GameTest server only: data in another namespace
 * ({@code void_reclaimer_test}) that uses the extension points the way an addon or datapack would.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GameTestPack {
    public static final String NAMESPACE = "void_reclaimer_test";

    private GameTestPack() {}

    @SubscribeEvent
    static void onAddPackFinders(AddPackFindersEvent event) {
        if (GameTestHooks.isGametestServer()) {
            event.addPackFinders(VoidReclaimer.id("gametest_pack"), PackType.SERVER_DATA,
                    Component.literal("Void Reclaimer GameTest pack"), PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }
}
