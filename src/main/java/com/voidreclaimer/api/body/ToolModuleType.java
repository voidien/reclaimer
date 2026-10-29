package com.voidreclaimer.api.body;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a tool module does. Tool modules are items with the {@code void_reclaimer:tool_module} component naming an
 * entry of the code registry {@code void_reclaimer:tool_module_type}. The active tool module (picked on the module
 * wheel) works whenever the main hand is empty.
 */
public interface ToolModuleType {
    /**
     * Right-click with an empty hand (server side; {@link com.voidreclaimer.api.event.ModuleUseEvent} has already
     * passed). {@code onBlock} is true when the player is looking at a block within reach.
     *
     * @return whether the module acted
     */
    default boolean use(ServerPlayer player, ItemStack module, boolean onBlock) {
        return false;
    }

    /** Mining speed while this module is active and the hand is empty; return {@code speed} to leave it unchanged. */
    default float breakSpeed(Player player, ItemStack module, BlockState state, float speed) {
        return speed;
    }
}
