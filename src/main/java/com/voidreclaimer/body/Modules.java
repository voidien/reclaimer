package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.nanite.NaniteTargeting;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Tool modules: the body's active tool (picked on the module wheel) works whenever the main hand is empty.
 * <ul>
 *   <li>Nanite Swarm: right-click builds in empty space ahead; sneak + right-click on a block builds against its
 *       face (a plain right-click on a block still uses the block).</li>
 *   <li>Salvage Tool: breaks salvageable blocks fast and refunds dust ({@link com.voidreclaimer.salvage.Salvage}).</li>
 * </ul>
 * Sensor modules work passively (see {@link com.voidreclaimer.dust.DustSensor}).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Modules {
    private static final float SALVAGE_SPEED = 20f;

    private Modules() {}

    /** The active tool module (empty when none). */
    public static ItemStack activeTool(Player player) {
        return Bodies.data(player).activeToolStack();
    }

    /** Whether {@code item} is the active tool and the hand is free to use it. */
    public static boolean isActive(Player player, Item item) {
        return player.getMainHandItem().isEmpty() && activeTool(player).is(item);
    }

    public static int naniteRange(Player player) {
        return Nanites.range(activeTool(player));
    }

    public static void setNaniteRange(ServerPlayer player, int range) {
        BodyData body = Bodies.data(player);
        ItemStack tool = body.activeToolStack();
        if (tool.is(ModItems.NANITE_SWARM.get())) {
            tool.set(ModDataComponents.NANITE_RANGE.get(), NaniteTargeting.clampRange(range));
            Bodies.store(player, body.with(PartKind.TOOL, body.activeTool(), tool));
        }
    }

    /** Uses the active tool module (sent by the client when right-clicking with an empty hand). */
    public static void use(ServerPlayer player) {
        if (!isActive(player, ModItems.NANITE_SWARM.get()) || player.getCooldowns().isOnCooldown(ModItems.NANITE_SWARM.get())) {
            return;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1f, false);
        Nanites.Result result;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            if (!player.isShiftKeyDown()) {
                return;
            }
            result = Nanites.buildAt(player, blockHit.getBlockPos().relative(blockHit.getDirection()), blockHit.getDirection());
        } else {
            result = Nanites.build(player, naniteRange(player));
        }
        if (result == Nanites.Result.BUILT) {
            player.getCooldowns().addCooldown(ModItems.NANITE_SWARM.get(), Nanites.BUILD_TICKS);
        }
    }

    /** Salvage module: fast on things the AI built. */
    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(ModBlocks.SALVAGEABLE) && isActive(event.getEntity(), ModItems.SALVAGE_TOOL.get())) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), SALVAGE_SPEED));
        }
    }

    @Nullable
    public static Item activeToolItem(Player player) {
        ItemStack tool = activeTool(player);
        return tool.isEmpty() ? null : tool.getItem();
    }
}
