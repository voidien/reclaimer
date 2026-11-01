package com.voidreclaimer.earth;

import com.mojang.serialization.Codec;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

/**
 * Which of the planet's {@link Sectors} are restored: one bit per sector, held in a saved, synced attachment on the
 * {@code ruined_earth} level. Pure, apart from {@link #of}.
 */
public record SectorMap(int restored) {
    public static final SectorMap NONE = new SectorMap(0);
    private static final int ALL = (1 << Sectors.COUNT) - 1;
    public static final Codec<SectorMap> CODEC = Codec.INT.xmap(SectorMap::new, SectorMap::restored);
    public static final StreamCodec<ByteBuf, SectorMap> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(SectorMap::new, SectorMap::restored);

    public SectorMap {
        restored &= ALL;
    }

    /** The map {@code level} holds; none restored on a level that has none, such as any client away from Earth. */
    public static SectorMap of(Level level) {
        SectorMap map = level.getExistingDataOrNull(ModAttachments.EARTH_SECTORS);
        return map == null ? NONE : map;
    }

    public boolean isRestored(int sector) {
        return sector >= 0 && sector < Sectors.COUNT && (restored >> sector & 1) != 0;
    }

    public SectorMap with(int sector) {
        return sector < 0 || sector >= Sectors.COUNT ? this : new SectorMap(restored | 1 << sector);
    }

    public SectorMap without(int sector) {
        return sector < 0 || sector >= Sectors.COUNT ? this : new SectorMap(restored & ~(1 << sector));
    }

    public int count() {
        return Integer.bitCount(restored);
    }

    /** How much of the planet is restored, 0 to 1. */
    public float fraction() {
        return count() / (float) Sectors.COUNT;
    }
}
