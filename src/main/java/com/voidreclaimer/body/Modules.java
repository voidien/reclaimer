package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ToolModuleType;
import com.voidreclaimer.api.event.ModuleUseEvent;
import com.voidreclaimer.nanite.NaniteTargeting;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Tool modules: the body's active tool (picked on the module wheel) works whenever the main hand is empty. What a
 * tool does comes from its {@link ToolModuleType} ({@link ToolModules} has the built-ins). Sensor modules work
 * passively (see {@link com.voidreclaimer.dust.DustSensor}).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Modules {
    private Modules() {}

    /** The active tool module's type when the hand is free to use it, or null. */
    public static ToolModuleType activeType(Player player) {
        return player.getMainHandItem().isEmpty() ? Parts.toolType(Bodies.activeTool(player)) : null;
    }

    public static int naniteRange(Player player) {
        return Nanites.range(Bodies.activeTool(player));
    }

    /** Sets the Nanite Swarm's build distance, stored on the active tool module. */
    public static void setNaniteRange(ServerPlayer player, int range) {
        BodyView view = Bodies.view(player);
        BodyData.Mount mount = view.activeToolMount();
        ItemStack tool = view.activeTool();
        if (mount != null && Parts.toolType(tool) == ToolModules.NANITE_SWARM.get()) {
            tool.set(ModDataComponents.NANITE_RANGE.get(), NaniteTargeting.clampRange(range));
            Bodies.store(player, view.data().withModule(mount, tool));
        }
    }

    /** Uses the active tool module (sent by the client when right-clicking with an empty hand). */
    public static void use(ServerPlayer player) {
        ItemStack tool = Bodies.activeTool(player);
        ToolModuleType type = player.getMainHandItem().isEmpty() ? Parts.toolType(tool) : null;
        if (type == null || NeoForge.EVENT_BUS.post(new ModuleUseEvent(player, tool, type)).isCanceled()) {
            return;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1f, false);
        type.use(player, tool, hit.getType() == HitResult.Type.BLOCK);
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ToolModuleType type = activeType(player);
        if (type != null) {
            event.setNewSpeed(type.breakSpeed(player, Bodies.activeTool(player), event.getState(), event.getNewSpeed()));
        }
    }

    /** Whether the body has any tool mounts (for the module wheel key). */
    public static boolean hasToolMounts(Player player) {
        return !Bodies.view(player).toolMounts().isEmpty();
    }
}
