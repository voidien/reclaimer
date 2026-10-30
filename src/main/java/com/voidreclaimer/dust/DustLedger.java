package com.voidreclaimer.dust;

import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps count of the dust a player has gathered over the whole game ({@link ModAttachments#DUST_COLLECTED}), which the
 * dust milestones look at. Gifts (starter dust) and refunds are not recorded here, so they never unlock anything.
 */
public final class DustLedger {
    private DustLedger() {}

    /** Records {@code amount} more dust of {@code kind} as gathered. */
    public static void record(ServerPlayer player, ResourceLocation kind, float amount) {
        if (amount <= 0f) {
            return;
        }
        DustData collected = player.getData(ModAttachments.DUST_COLLECTED);
        player.setData(ModAttachments.DUST_COLLECTED, collected.with(kind, collected.get(kind) + amount));
    }

    /** Raises the record of {@code kind} to at least {@code amount} (the dust command uses this so test setups still unlock milestones). */
    public static void raiseTo(ServerPlayer player, ResourceLocation kind, float amount) {
        DustData collected = player.getData(ModAttachments.DUST_COLLECTED);
        if (collected.get(kind) < amount) {
            player.setData(ModAttachments.DUST_COLLECTED, collected.with(kind, amount));
        }
    }
}
