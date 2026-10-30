package com.voidreclaimer.earth;

import com.mojang.serialization.Codec;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

/**
 * Which of the three skies ruined Earth shows. One era for the whole dimension, stored as a saved, synced attachment on
 * the {@code ruined_earth} level. For now a debug command sets it; later the Spires' restoration percent drives it.
 */
public enum SkyEra implements StringRepresentable {
    RUINED("ruined"),
    HEALING("healing"),
    RESTORED("restored");

    public static final Codec<SkyEra> CODEC = StringRepresentable.fromEnum(SkyEra::values);
    public static final StreamCodec<ByteBuf, SkyEra> STREAM_CODEC = ByteBufCodecs.idMapper(index -> values()[index], SkyEra::ordinal);

    private final String name;

    SkyEra(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The era shown in {@code level}: the synced attachment, or {@link #RUINED} when nothing was set. */
    public static SkyEra of(Level level) {
        SkyEra era = level.getExistingDataOrNull(ModAttachments.EARTH_SKY_ERA);
        return era == null ? RUINED : era;
    }
}
