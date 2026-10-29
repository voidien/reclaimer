package com.voidreclaimer.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the game bus (server side) each tick before dust is added from a cloud. Change the amount, or cancel to
 * add nothing. The amount is clamped to the player's capacity afterwards.
 */
public class DustGainEvent extends PlayerEvent implements ICancellableEvent {
    private final ResourceLocation kind;
    private final double density;
    private float amount;

    public DustGainEvent(Player player, ResourceLocation kind, double density, float amount) {
        super(player);
        this.kind = kind;
        this.density = density;
        this.amount = amount;
    }

    /** The dust kind id of the cloud. */
    public ResourceLocation getKind() {
        return kind;
    }

    /** Cloud density 0..1 at the player. */
    public double getDensity() {
        return density;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = Math.max(0f, amount);
    }
}
