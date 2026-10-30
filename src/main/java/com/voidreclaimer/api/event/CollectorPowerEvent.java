package com.voidreclaimer.api.event;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;

/**
 * Fired on the game bus (server side) about once a second for every Cloud Siphon, to decide how hard it works.
 * {@code power} starts as what its shaft gives (1.0 at 64 RPM, proportional to speed, 0 below the minimum speed); an
 * addon that powers the block another way (electricity, say) raises it. The siphon's output scales with it.
 */
public class CollectorPowerEvent extends Event {
    private final BlockEntity blockEntity;
    private float power;

    public CollectorPowerEvent(BlockEntity blockEntity, float power) {
        this.blockEntity = blockEntity;
        this.power = power;
    }

    /** The Cloud Siphon asking. */
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public float getPower() {
        return power;
    }

    /** Replaces the power; negative values count as zero. */
    public void setPower(float power) {
        this.power = Math.max(0f, power);
    }
}
