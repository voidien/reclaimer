package com.voidreclaimer.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModCodecs;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A level's weather. Saved with the level and synced to everyone in it.
 *
 * @param weather     what is coming or here, if anything
 * @param warningLeft seconds until it starts
 * @param activeLeft  seconds until it ends, once it has started
 * @param strength    how strong this one is, 0 to 1
 * @param nextIn      seconds until the next one is picked; below 0 means none is planned
 */
public record WeatherState(Optional<ResourceLocation> weather, int warningLeft, int activeLeft, float strength, int nextIn) {
    public static final WeatherState NONE = new WeatherState(Optional.empty(), 0, 0, 0f, -1);

    public static final Codec<WeatherState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModCodecs.ID.lenientOptionalFieldOf("weather").forGetter(WeatherState::weather),
            Codec.INT.optionalFieldOf("warning_left", 0).forGetter(WeatherState::warningLeft),
            Codec.INT.optionalFieldOf("active_left", 0).forGetter(WeatherState::activeLeft),
            Codec.FLOAT.optionalFieldOf("strength", 0f).forGetter(WeatherState::strength),
            Codec.INT.optionalFieldOf("next_in", -1).forGetter(WeatherState::nextIn)
    ).apply(instance, WeatherState::new));

    public static final StreamCodec<ByteBuf, WeatherState> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), WeatherState::weather,
            ByteBufCodecs.VAR_INT, WeatherState::warningLeft,
            ByteBufCodecs.VAR_INT, WeatherState::activeLeft,
            ByteBufCodecs.FLOAT, WeatherState::strength,
            ByteBufCodecs.INT, WeatherState::nextIn,
            WeatherState::new);

    public boolean warning() {
        return weather.isPresent() && warningLeft > 0;
    }

    public boolean active() {
        return weather.isPresent() && warningLeft <= 0 && activeLeft > 0;
    }
}
