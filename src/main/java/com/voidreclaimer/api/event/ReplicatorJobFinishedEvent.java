package com.voidreclaimer.api.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the game bus (server side) when a Replicator job finishes, before its items are delivered. The output list
 * may be changed: add, remove or replace stacks. Whatever doesn't fit in the inventory goes to the output buffer.
 */
public class ReplicatorJobFinishedEvent extends PlayerEvent {
    private final ResourceLocation recipe;
    private final List<ItemStack> outputs;

    public ReplicatorJobFinishedEvent(Player player, ResourceLocation recipe, List<ItemStack> outputs) {
        super(player);
        this.recipe = recipe;
        this.outputs = new ArrayList<>(outputs);
    }

    /** Id of the recipe the job was queued from. */
    public ResourceLocation getRecipe() {
        return recipe;
    }

    /** The items to deliver; mutable. */
    public List<ItemStack> getOutputs() {
        return outputs;
    }
}
