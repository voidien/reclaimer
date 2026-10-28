package com.voidreclaimer.dust;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side dust collection. Inside a cloud the AI scoops dust of the cloud's kind: a slow trickle while still,
 * much more while flying through it. Both scale with local density. Dust never gives energy.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class DustTicker {
    private static final int PERIODIC_SYNC_TICKS = 20;

    private DustTicker() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        DustField field = DustField.of(player.level());
        if (field == null) {
            return;
        }
        DustField.Sample sample = field.sample(player.getBoundingBox().getCenter());
        if (!sample.inCloud()) {
            return;
        }
        double speed = player.getData(ModAttachments.FLIGHT_TRACKER).lastSpeed();
        float gain = gainPerTick(sample.density(), speed, Config.DUST_TRICKLE.get(), Config.DUST_SCOOP.get());
        DustData old = player.getData(ModAttachments.DUST);
        DustKind kind = sample.cloud().kind();
        DustData updated = old.add(kind, gain, DustData.capacity(player));
        if (updated.equals(old)) {
            return;
        }
        player.setData(ModAttachments.DUST, updated);
        if (Mth.floor(updated.get(kind)) != Mth.floor(old.get(kind)) || player.tickCount % PERIODIC_SYNC_TICKS == 0) {
            player.syncData(ModAttachments.DUST);
        }
    }

    /**
     * Dust gained in one tick.
     *
     * @param density       local cloud density 0..1
     * @param speed         blocks moved this tick
     * @param tricklePerSec dust per second at full density while still
     * @param scoopPerBlock dust per block travelled at full density
     */
    public static float gainPerTick(double density, double speed, double tricklePerSec, double scoopPerBlock) {
        return (float) (density * (tricklePerSec / 20.0 + scoopPerBlock * speed));
    }
}
