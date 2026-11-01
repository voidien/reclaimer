package com.voidreclaimer.energy;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side energy simulation. The AI does not eat: vanilla hunger is pinned full and the HUD shows energy instead.
 *
 * <p>Above the emergency reserve energy drains slowly. Below it, idle drain stops and an always-on trickle charge
 * starts, so idling in darkness can never strand the player. Sunlight charges in proportion to effective sky light.
 * Hitting zero (only possible through active use such as thrust, from Phase 2 on) enters low power mode until energy
 * recovers to the restart threshold.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class EnergyTicker {
    private static final int PERIODIC_SYNC_TICKS = 20;

    private EnergyTicker() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        pinHunger(player.getFoodData());
        if (Energy.isExempt(player)) {
            return;
        }

        EnergyData updated = simulate(player.getData(ModAttachments.ENERGY), sunExposure(player), EnergyRules.forBody(com.voidreclaimer.body.Bodies.stats(player)));
        Energy.update(player, updated, player.tickCount % PERIODIC_SYNC_TICKS == 0);
    }

    /** One tick of energy simulation. */
    public static EnergyData simulate(EnergyData data, float sunExposure, EnergyRules rules) {
        float capacity = rules.capacity();
        float energy = Math.min(data.energy(), capacity);

        energy += sunExposure * rules.sunChargePerTick();
        if (energy > capacity * rules.reserveFraction()) {
            energy -= rules.idleDrainPerTick();
        } else {
            energy += rules.emergencyChargePerTick();
        }
        energy = Mth.clamp(energy, 0f, capacity);

        boolean lowPower = data.lowPower();
        if (energy <= 0f) {
            lowPower = true;
        } else if (lowPower && energy >= capacity * rules.restartFraction()) {
            lowPower = false;
        }
        return new EnergyData(energy, capacity, lowPower);
    }

    /**
     * 0..1 sunlight at the player's eyes: raw sky light minus the level's current sky darkening, over full daylight.
     * Deep space's fixed dim sun gives about 2/3 in the open; anything shaded or enclosed gets less or none.
     */
    static float sunExposure(ServerPlayer player) {
        return sunExposure(player.level(), BlockPos.containing(player.getEyePosition()));
    }

    /** 0..1 sunlight at a block position: anything that runs on sunlight reads the same light as the body. */
    public static float sunExposure(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) {
            return 0f;
        }
        int effective = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        return Mth.clamp(effective / 15f, 0f, 1f);
    }

    /** Vanilla food never matters: keep it full with no saturation so health regenerates at the normal slow rate. */
    private static void pinHunger(FoodData food) {
        if (food.getFoodLevel() != 20) {
            food.setFoodLevel(20);
        }
        food.setSaturation(0f);
        food.setExhaustion(0f);
    }
}
