package com.voidreclaimer.energy;

import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Entry point for systems that use the AI's energy (thrusters, dampener, and later tools and the replicator). */
public final class Energy {
    private Energy() {}

    /** Creative and spectator players neither use nor need energy. */
    public static boolean isExempt(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    /** Whether powered systems work. Valid on both sides; the client reads the synced attachment. */
    public static boolean canPowerSystems(Player player) {
        return isExempt(player) || !player.getData(ModAttachments.ENERGY).lowPower();
    }

    /** Removes energy, entering low power mode at zero. Does nothing for exempt players. */
    public static void consume(ServerPlayer player, float amount) {
        if (isExempt(player) || amount <= 0f) {
            return;
        }
        EnergyData old = player.getData(ModAttachments.ENERGY);
        float energy = Math.max(0f, old.energy() - amount);
        update(player, new EnergyData(energy, old.capacity(), old.lowPower() || energy <= 0f), false);
    }

    /**
     * Stores new energy data, announces low power transitions, and syncs to the client when the change is visible
     * (or when {@code forceSync} is set).
     */
    public static void update(ServerPlayer player, EnergyData updated, boolean forceSync) {
        EnergyData old = player.getData(ModAttachments.ENERGY);
        player.setData(ModAttachments.ENERGY, updated);
        boolean lowPowerChanged = updated.lowPower() != old.lowPower();
        if (lowPowerChanged) {
            player.displayClientMessage(updated.lowPower()
                    ? Component.translatable("message.void_reclaimer.low_power").withStyle(ChatFormatting.RED)
                    : Component.translatable("message.void_reclaimer.power_restored").withStyle(ChatFormatting.AQUA), true);
        }
        if (forceSync || lowPowerChanged || updated.halfIcons() != old.halfIcons() || updated.capacity() != old.capacity()) {
            player.syncData(ModAttachments.ENERGY);
        }
    }
}
