package com.voidreclaimer.weather;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.event.SpaceWeatherEvent;
import com.voidreclaimer.api.weather.SpaceWeather;
import com.voidreclaimer.api.weather.WeatherEffectType;
import com.voidreclaimer.api.weather.WeatherExposureType;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModRegistries;
import com.voidreclaimer.replicator.Replicator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Runs each level's weather, once a second. A level with nobody in it has no weather to speak of, so its clock stops. Each kind is a file
 * in {@code void_reclaimer:space_weather}: this only keeps time and hands each exposed body to the kind's effects.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class SpaceWeatherTicker {
    private static final int EVERY_TICKS = 20;
    private static final int SECONDS_A_HOUR = 3600;
    private static final java.util.Set<ResourceLocation> MISSING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private SpaceWeatherTicker() {}

    public static WeatherState of(ServerLevel level) {
        return level.getData(ModAttachments.WEATHER);
    }

    /** What is here now, not what is on its way. */
    public static Optional<SpaceWeather> active(ServerLevel level) {
        WeatherState state = of(level);
        return state.active() ? state.weather().flatMap(registry(level)::getOptional) : Optional.empty();
    }

    /** How much of a remote link's signal the weather in {@code level} is taking now. */
    public static double signalLoss(ServerLevel level) {
        return active(level).map(weather -> (double) weather.signalLoss() * of(level).strength()).orElse(0.0);
    }

    public static Registry<SpaceWeather> registry(ServerLevel level) {
        return level.registryAccess().registryOrThrow(VoidReclaimerRegistries.SPACE_WEATHER);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % EVERY_TICKS == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                if (!Config.WEATHER_ENABLED.get()) {
                    if (of(level).weather().isPresent()) {
                        stop(level);
                    }
                } else if (!level.players().isEmpty()) {
                    second(level);
                }
            }
        }
    }

    /** One second of {@code level}'s weather. */
    public static void second(ServerLevel level) {
        WeatherState before = of(level);
        RandomSource random = level.getRandom();
        WeatherState after = WeatherClock.second(before, candidates(level), () -> gap(random), random::nextInt);
        Optional<ResourceLocation> id = after.weather().or(before::weather);
        Optional<SpaceWeather> weather = id.flatMap(registry(level)::getOptional);
        if (!before.weather().equals(after.weather()) && after.weather().isPresent() && weather.isPresent()
                && NeoForge.EVENT_BUS.post(new SpaceWeatherEvent.Warning(level, id.get(), weather.get())).isCanceled()) {
            level.setData(ModAttachments.WEATHER, new WeatherState(Optional.empty(), 0, 0, 0f, gap(random)));
            return;
        }
        level.setData(ModAttachments.WEATHER, after);
        if (id.isPresent() && weather.isEmpty()) {
            // Its file has gone from the datapack: nothing can say what it does.
            level.setData(ModAttachments.WEATHER, new WeatherState(Optional.empty(), 0, 0, 0f, gap(random)));
            return;
        }
        if (weather.isPresent()) {
            if (!before.active() && after.active()) {
                NeoForge.EVENT_BUS.post(new SpaceWeatherEvent.Started(level, id.get(), weather.get()));
            }
            if (after.active()) {
                reach(level, id.get(), weather.get(), after);
            }
            if (before.active() && !after.active()) {
                NeoForge.EVENT_BUS.post(new SpaceWeatherEvent.Ended(level, id.get(), weather.get()));
            }
        }
    }

    private static void reach(ServerLevel level, ResourceLocation id, SpaceWeather weather, WeatherState state) {
        WeatherExposureType exposure = ModRegistries.WEATHER_EXPOSURE_TYPES.get(weather.exposure());
        if (exposure == null) {
            missing(weather.exposure());
            return;
        }
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            float share = exposure.exposure(player);
            SpaceWeatherEvent.Exposed event = new SpaceWeatherEvent.Exposed(level, id, weather, player, share);
            if (share > 0f && !NeoForge.EVENT_BUS.post(event).isCanceled() && event.getShare() > 0f) {
                for (SpaceWeather.Effect effect : weather.effects()) {
                    WeatherEffectType type = ModRegistries.WEATHER_EFFECT_TYPES.get(effect.type());
                    if (type == null) {
                        missing(effect.type());
                    } else if (player.isAlive()) {
                        type.second(player, effect.strength() * state.strength() * event.getShare());
                    }
                }
            }
        }
    }

    /** A file names something that isn't there, most likely a typo. Once, or a storm would fill the log. */
    private static void missing(ResourceLocation id) {
        if (MISSING.add(id)) {
            VoidReclaimer.LOGGER.warn("Space weather names {}, which does not exist, so that part of it does nothing", id);
        }
    }

    /** What could come to {@code level} now: it happens here, and someone here has reached its milestone. */
    private static List<WeatherClock.Candidate> candidates(ServerLevel level) {
        List<WeatherClock.Candidate> candidates = new ArrayList<>();
        Registry<SpaceWeather> registry = registry(level);
        for (var entry : registry.entrySet()) {
            SpaceWeather weather = entry.getValue();
            if (weather.dimensions().contains(level.dimension()) && weather.milestone().map(milestone -> reached(level, milestone)).orElse(true)) {
                candidates.add(candidate(entry.getKey().location(), weather));
            }
        }
        return candidates;
    }

    static WeatherClock.Candidate candidate(ResourceLocation id, SpaceWeather weather) {
        return new WeatherClock.Candidate(id, weather.weight(), weather.warningSeconds(), weather.minSeconds(), weather.maxSeconds());
    }

    private static boolean reached(ServerLevel level, ResourceLocation milestone) {
        ResourceKey<com.voidreclaimer.api.milestone.Milestone> key = ResourceKey.create(VoidReclaimerRegistries.MILESTONE, milestone);
        return level.players().stream().anyMatch(player -> Replicator.data(player).isUnlocked(key));
    }

    private static int gap(RandomSource random) {
        double low = Config.WEATHER_GAP_MIN_HOURS.get();
        double high = Math.max(low, Config.WEATHER_GAP_MAX_HOURS.get());
        return (int) ((low + random.nextDouble() * (high - low)) * SECONDS_A_HOUR);
    }

    /** Starts {@code id} now, warning and all, for a command, ending what was here. False if there is no such kind, or a listener said no. */
    public static boolean start(ServerLevel level, ResourceLocation id) {
        Optional<SpaceWeather> weather = registry(level).getOptional(id);
        if (weather.isEmpty()) {
            return false;
        }
        stop(level);
        if (NeoForge.EVENT_BUS.post(new SpaceWeatherEvent.Warning(level, id, weather.get())).isCanceled()) {
            return false;
        }
        WeatherState state = WeatherClock.start(candidate(id, weather.get()), level.getRandom()::nextInt);
        level.setData(ModAttachments.WEATHER, state);
        if (state.active()) {
            NeoForge.EVENT_BUS.post(new SpaceWeatherEvent.Started(level, id, weather.get()));
        }
        return true;
    }

    /** Ends what is here or coming, for a command. */
    public static void stop(ServerLevel level) {
        WeatherState state = of(level);
        boolean wasActive = state.active();
        level.setData(ModAttachments.WEATHER, new WeatherState(Optional.empty(), 0, 0, 0f, gap(level.getRandom())));
        if (wasActive) {
            state.weather().flatMap(id -> registry(level).getOptional(id).map(weather -> new SpaceWeatherEvent.Ended(level, id, weather)))
                    .ifPresent(NeoForge.EVENT_BUS::post);
        }
    }
}
