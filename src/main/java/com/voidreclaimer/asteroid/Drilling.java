package com.voidreclaimer.asteroid;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ToolModuleType;
import com.voidreclaimer.body.Modules;
import com.voidreclaimer.body.ToolModules;
import com.voidreclaimer.body.Wear;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Asteroid rock only breaks to a powered Mechanical Drill or Matter Harvester (active tool module, empty hand).
 * The drill removes the block and refunds a little dust of its kind; the harvester lets the block drop as an item.
 * Creative players break anything. The speed rule runs on both sides so the client doesn't even start digging; the
 * break rule is the server's own check.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Drilling {
    private Drilling() {}

    /** Whether the player's active tool module is a powered drill or harvester. */
    public static boolean canDig(Player player) {
        ToolModuleType type = Modules.activeType(player);
        return (type == ToolModules.DRILL.get() || type == ToolModules.HARVESTER.get()) && Energy.canPowerSystems(player);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (event.getState().is(ModBlocks.ASTEROID_BLOCKS) && !player.isCreative() && !canDig(player)) {
            event.setNewSpeed(0f);
        }
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative() || !state.is(ModBlocks.ASTEROID_BLOCKS)) {
            return;
        }
        if (!canDig(player)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.void_reclaimer.drill.needed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Energy.consume(player, Config.DRILL_ENERGY.get().floatValue());
        Wear.onToolUse(player);
        if (Modules.activeType(player) == ToolModules.DRILL.get() && state.getBlock() instanceof AsteroidBlock rock) {
            // The drill grinds the rock to dust: no drop, a refund of the block's own kind.
            event.setCanceled(true);
            ServerLevel level = player.serverLevel();
            level.destroyBlock(event.getPos(), false, player);
            DustData dust = player.getData(ModAttachments.DUST);
            DustData gained = dust.add(rock.dustKind(), Config.DRILL_DUST_YIELD.get().floatValue(), DustData.capacity(player));
            player.setData(ModAttachments.DUST, gained);
            com.voidreclaimer.dust.DustLedger.record(player, rock.dustKind(), gained.get(rock.dustKind()) - dust.get(rock.dustKind()));
            player.syncData(ModAttachments.DUST);
        }
    }
}
