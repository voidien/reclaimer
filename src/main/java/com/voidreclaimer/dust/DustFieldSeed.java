package com.voidreclaimer.dust;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Seed of the deep space dust field, stored as a synced attachment on the deep space level so the client can
 * evaluate the same clouds as the server. It is derived from the world seed through a one-way mix, so the world
 * seed itself never reaches clients. Not saved: it is recomputed whenever the level loads.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public record DustFieldSeed(long seed) {
    public static final StreamCodec<ByteBuf, DustFieldSeed> STREAM_CODEC =
            ByteBufCodecs.VAR_LONG.map(DustFieldSeed::new, DustFieldSeed::seed);

    private static final long SALT = 0xC05_41C_D057L;

    public static long derive(long worldSeed) {
        long h = worldSeed ^ SALT;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && ModDimensions.isDeepSpace(level)) {
            level.setData(ModAttachments.DUST_FIELD_SEED, new DustFieldSeed(derive(level.getSeed())));
        }
    }
}
