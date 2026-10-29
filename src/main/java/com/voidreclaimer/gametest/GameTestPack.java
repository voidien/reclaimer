package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.ToolModuleType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.RegisterEvent;
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

    /** A tool module type an addon might register: counts its uses per player. */
    public static final ResourceKey<ToolModuleType> MARKER = ResourceKey.create(VoidReclaimerRegistries.TOOL_MODULE_TYPE,
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "marker"));
    static final Map<UUID, Integer> MARKER_USES = new ConcurrentHashMap<>();

    private GameTestPack() {}

    @SubscribeEvent
    static void onRegister(RegisterEvent event) {
        if (GameTestHooks.isGametestServer()) {
            event.register(VoidReclaimerRegistries.TOOL_MODULE_TYPE, helper -> helper.register(MARKER, new ToolModuleType() {
                @Override
                public boolean use(ServerPlayer player, ItemStack module, boolean onBlock) {
                    MARKER_USES.merge(player.getUUID(), 1, Integer::sum);
                    return true;
                }
            }));
        }
    }

    @SubscribeEvent
    static void onAddPackFinders(AddPackFindersEvent event) {
        if (GameTestHooks.isGametestServer()) {
            event.addPackFinders(VoidReclaimer.id("gametest_pack"), PackType.SERVER_DATA,
                    Component.literal("Void Reclaimer GameTest pack"), PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }
}
