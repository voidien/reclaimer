package com.voidreclaimer.api.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModCodecs;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * One kind of bad weather, in the synced datapack registry {@code void_reclaimer:space_weather}. A new hazard is one of these files, and at
 * most one new {@link WeatherEffectType}.
 *
 * @param dimensions     where it happens
 * @param always         whether it never stops, like a planet's bad air, instead of coming and going. Such a kind only reaches bodies: its
 *                       {@code signalLoss} and {@code scrambles} do nothing, as they belong to a storm's time in a level
 * @param weight         how often it comes up against the others; 0 means only by command
 * @param milestone      the milestone a player in the dimension must have reached before it comes
 * @param warningSeconds how long the warning runs before it starts
 * @param minSeconds     the shortest it lasts
 * @param maxSeconds     the longest it lasts
 * @param exposure       what shelters a body, from {@link VoidReclaimerRegistries#WEATHER_EXPOSURE_TYPE}
 * @param effects        what it does each second to a body it reaches
 * @param signalLoss     how much of a remote link's signal it takes at full strength, 0 to 1: a link left with none is lost
 * @param scrambles      whether sensors and the HUD go to static while it lasts
 * @param color          the HUD's colour, as 0xRRGGBB
 * @param name           what the HUD calls it
 */
public record SpaceWeather(List<ResourceKey<Level>> dimensions, boolean always, int weight, Optional<ResourceLocation> milestone, int warningSeconds, int minSeconds,
                           int maxSeconds, ResourceLocation exposure, List<Effect> effects, float signalLoss, boolean scrambles, int color, Optional<Component> name) {
    /**
     * One thing it does.
     *
     * @param type     from {@link VoidReclaimerRegistries#WEATHER_EFFECT_TYPE}
     * @param strength how hard, in the type's own terms, at full strength and full exposure
     */
    public record Effect(ResourceLocation type, float strength) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ModCodecs.ID.fieldOf("type").forGetter(Effect::type),
                Codec.FLOAT.fieldOf("strength").forGetter(Effect::strength)
        ).apply(instance, Effect::new));
    }

    public static final Codec<SpaceWeather> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).listOf().fieldOf("dimensions").forGetter(SpaceWeather::dimensions),
            Codec.BOOL.optionalFieldOf("always", false).forGetter(SpaceWeather::always),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(SpaceWeather::weight),
            ModCodecs.ID.optionalFieldOf("milestone").forGetter(SpaceWeather::milestone),
            Codec.INT.optionalFieldOf("warning_seconds", 30).forGetter(SpaceWeather::warningSeconds),
            Codec.INT.optionalFieldOf("min_seconds", 0).forGetter(SpaceWeather::minSeconds),
            Codec.INT.optionalFieldOf("max_seconds", 0).forGetter(SpaceWeather::maxSeconds),
            ModCodecs.ID.fieldOf("exposure").forGetter(SpaceWeather::exposure),
            Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SpaceWeather::effects),
            Codec.FLOAT.optionalFieldOf("signal_loss", 0f).forGetter(SpaceWeather::signalLoss),
            Codec.BOOL.optionalFieldOf("scrambles", false).forGetter(SpaceWeather::scrambles),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(SpaceWeather::color),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(SpaceWeather::name)
    ).apply(instance, SpaceWeather::new));
}
