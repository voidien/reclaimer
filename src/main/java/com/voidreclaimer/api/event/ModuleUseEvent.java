package com.voidreclaimer.api.event;

import com.voidreclaimer.api.body.ToolModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Fired on the game bus (server side) before the active tool module is used. Cancel to stop it. */
public class ModuleUseEvent extends PlayerEvent implements ICancellableEvent {
    private final ItemStack module;
    private final ToolModuleType type;

    public ModuleUseEvent(Player player, ItemStack module, ToolModuleType type) {
        super(player);
        this.module = module;
        this.type = type;
    }

    public ItemStack getModule() {
        return module;
    }

    public ToolModuleType getType() {
        return type;
    }
}
