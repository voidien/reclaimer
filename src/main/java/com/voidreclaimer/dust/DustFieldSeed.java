package com.voidreclaimer.dust;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.SpawnHandler;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Seed of the deep space dust field, stored as a synced attachment on the deep space level so the client can
 * evaluate the same clouds as the server. It is derived from the world seed through a one-way mix, so the world
 * seed itself never reaches clients. Not saved: it is recomputed whenever the level loads.
 *
 * @param seed    dust field seed
 * @param nebula  center of the {@link StarterNebula}, if this world has one
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public record DustFieldSeed(long seed, Optional<Vec3> nebula) {
    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    public static final StreamCodec<ByteBuf, DustFieldSeed> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DustFieldSeed::seed,
            VEC3.apply(ByteBufCodecs::optional), DustFieldSeed::nebula,
            DustFieldSeed::new);

    private static final long SALT = 0xC05_41C_D057L;

    public DustFieldSeed(long seed) {
        this(seed, Optional.empty());
    }

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
            if (!level.hasData(ModAttachments.STARTER_NEBULA)) {
                // First load of this world's deep space: lock in the game rule chosen at world creation.
                boolean enabled = level.getServer().getGameRules().getBoolean(StarterNebula.RULE);
                level.setData(ModAttachments.STARTER_NEBULA, new StarterNebula.Locked(enabled));
            }
            long seed = derive(level.getSeed());
            Optional<Vec3> nebula = level.getData(ModAttachments.STARTER_NEBULA).enabled()
                    ? Optional.of(StarterNebula.center(seed, SpawnHandler.spawnPosition()))
                    : Optional.empty();
            level.setData(ModAttachments.DUST_FIELD_SEED, new DustFieldSeed(seed, nebula));
        }
    }
}
