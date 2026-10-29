package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.ToolModuleType;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModBlocks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The built-in tool module types. */
public final class ToolModules {
    public static final DeferredRegister<ToolModuleType> TYPES =
            DeferredRegister.create(VoidReclaimerRegistries.TOOL_MODULE_TYPE, VoidReclaimer.MOD_ID);

    /**
     * Nanite Swarm: right-click builds in empty space ahead; sneak + right-click on a block builds against its face (a
     * plain right-click on a block still uses the block).
     */
    public static final DeferredHolder<ToolModuleType, ToolModuleType> NANITE_SWARM = TYPES.register("nanite_swarm", () -> new ToolModuleType() {
        @Override
        public boolean use(ServerPlayer player, ItemStack module, boolean onBlock, boolean sneak) {
            if (player.getCooldowns().isOnCooldown(module.getItem())) {
                return false;
            }
            HitResult hit = player.pick(player.blockInteractionRange(), 1f, false);
            Nanites.Result result;
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                if (!sneak) {
                    return false;
                }
                result = Nanites.buildAt(player, blockHit.getBlockPos().relative(blockHit.getDirection()), blockHit.getDirection());
            } else {
                result = Nanites.build(player, Nanites.range(module));
            }
            if (result == Nanites.Result.BUILT) {
                player.getCooldowns().addCooldown(module.getItem(), Nanites.BUILD_TICKS);
                return true;
            }
            return false;
        }
    });

    /** Salvage Tool: breaks salvageable blocks fast; the refund is in {@link com.voidreclaimer.salvage.Salvage}. */
    public static final DeferredHolder<ToolModuleType, ToolModuleType> SALVAGE = TYPES.register("salvage", () -> new ToolModuleType() {
        @Override
        public float breakSpeed(Player player, ItemStack module, BlockState state, float speed) {
            return state.is(ModBlocks.SALVAGEABLE) ? Math.max(speed, SALVAGE_SPEED) : speed;
        }
    });

    /** Mechanical Drill: breaks asteroid rock fast; the dust refund is in {@link com.voidreclaimer.asteroid.Drilling}. */
    public static final DeferredHolder<ToolModuleType, ToolModuleType> DRILL = TYPES.register("drill", () -> new ToolModuleType() {
        @Override
        public float breakSpeed(Player player, ItemStack module, BlockState state, float speed) {
            return state.is(ModBlocks.ASTEROID_BLOCKS) ? DRILL_SPEED : speed;
        }
    });

    /** Matter Harvester: takes asteroid rock out whole, as the block item, more slowly than the drill. */
    public static final DeferredHolder<ToolModuleType, ToolModuleType> HARVESTER = TYPES.register("harvester", () -> new ToolModuleType() {
        @Override
        public float breakSpeed(Player player, ItemStack module, BlockState state, float speed) {
            return state.is(ModBlocks.ASTEROID_BLOCKS) ? HARVESTER_SPEED : speed;
        }
    });

    private static final float SALVAGE_SPEED = 20f;
    private static final float DRILL_SPEED = 10f;
    private static final float HARVESTER_SPEED = 4f;

    private ToolModules() {}

    public static ResourceKey<ToolModuleType> key(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.TOOL_MODULE_TYPE, VoidReclaimer.id(name));
    }

    /** Whether the active tool module is of {@code type} and the hand is free to use it. */
    public static boolean isActive(Player player, DeferredHolder<ToolModuleType, ToolModuleType> type) {
        return player.getMainHandItem().isEmpty() && Parts.toolType(Bodies.activeTool(player)) == type.get();
    }
}
