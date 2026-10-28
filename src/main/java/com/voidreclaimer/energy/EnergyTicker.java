package com.voidreclaimer.energy;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
    private static final int TICKS_PER_SECOND = 20;
    private static final int PERIODIC_SYNC_TICKS = 20;

    private EnergyTicker() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        pinHunger(player.getFoodData());
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        EnergyData old = player.getData(ModAttachments.ENERGY);
        EnergyData updated = simulate(old, sunExposure(player));
        player.setData(ModAttachments.ENERGY, updated);

        if (updated.lowPower() != old.lowPower()) {
            player.displayClientMessage(updated.lowPower()
                    ? Component.translatable("message.void_reclaimer.low_power").withStyle(ChatFormatting.RED)
                    : Component.translatable("message.void_reclaimer.power_restored").withStyle(ChatFormatting.AQUA), true);
        }
        if (updated.lowPower() != old.lowPower()
                || updated.halfIcons() != old.halfIcons()
                || updated.capacity() != old.capacity()
                || player.tickCount % PERIODIC_SYNC_TICKS == 0) {
            player.syncData(ModAttachments.ENERGY);
        }
    }

    /** One tick of energy simulation. */
    static EnergyData simulate(EnergyData data, float sunExposure) {
        float capacity = Config.ENERGY_CAPACITY.get().floatValue();
        float reserve = capacity * Config.ENERGY_RESERVE_FRACTION.get().floatValue();
        float energy = Math.min(data.energy(), capacity);

        energy += sunExposure * perTick(Config.ENERGY_SUN_CHARGE.get());
        if (energy > reserve) {
            energy -= perTick(Config.ENERGY_IDLE_DRAIN.get());
        } else {
            energy += perTick(Config.ENERGY_EMERGENCY_CHARGE.get());
        }
        energy = Mth.clamp(energy, 0f, capacity);

        boolean lowPower = data.lowPower();
        if (energy <= 0f) {
            lowPower = true;
        } else if (lowPower && energy >= capacity * Config.ENERGY_RESTART_FRACTION.get().floatValue()) {
            lowPower = false;
        }
        return new EnergyData(energy, capacity, lowPower);
    }

    /**
     * 0..1 sunlight at the player's eyes: raw sky light minus the level's current sky darkening, over full daylight.
     * Deep space's fixed dim sun gives about 2/3 in the open; anything shaded or enclosed gets less or none.
     */
    static float sunExposure(ServerPlayer player) {
        Level level = player.level();
        if (!level.dimensionType().hasSkyLight()) {
            return 0f;
        }
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        int effective = level.getBrightness(LightLayer.SKY, eyes) - level.getSkyDarken();
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

    private static float perTick(double perSecond) {
        return (float) (perSecond / TICKS_PER_SECOND);
    }
}
