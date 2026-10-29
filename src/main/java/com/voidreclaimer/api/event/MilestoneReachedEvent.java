package com.voidreclaimer.api.event;

import com.voidreclaimer.api.milestone.Milestone;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the game bus (server side) after a player reaches a milestone through play (not through commands). The
 * milestone is already unlocked and synced when this fires.
 */
public class MilestoneReachedEvent extends PlayerEvent {
    private final ResourceKey<Milestone> milestone;

    public MilestoneReachedEvent(Player player, ResourceKey<Milestone> milestone) {
        super(player);
        this.milestone = milestone;
    }

    public ResourceKey<Milestone> getMilestone() {
        return milestone;
    }
}
