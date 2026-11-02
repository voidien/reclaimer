package com.voidreclaimer.api.event;

import com.voidreclaimer.api.weather.SpaceWeather;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Weather, on the game bus and the server side. */
public abstract class SpaceWeatherEvent extends Event {
    private final ServerLevel level;
    private final ResourceLocation id;
    private final SpaceWeather weather;

    protected SpaceWeatherEvent(ServerLevel level, ResourceLocation id, SpaceWeather weather) {
        this.level = level;
        this.id = id;
        this.weather = weather;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public ResourceLocation getId() {
        return id;
    }

    public SpaceWeather getWeather() {
        return weather;
    }

    /** It is on its way, and the warning has started. Cancel it and it never comes. */
    public static class Warning extends SpaceWeatherEvent implements ICancellableEvent {
        public Warning(ServerLevel level, ResourceLocation id, SpaceWeather weather) {
            super(level, id, weather);
        }
    }

    /** It has started. */
    public static class Started extends SpaceWeatherEvent {
        public Started(ServerLevel level, ResourceLocation id, SpaceWeather weather) {
            super(level, id, weather);
        }
    }

    /** It has ended. */
    public static class Ended extends SpaceWeatherEvent {
        public Ended(ServerLevel level, ResourceLocation id, SpaceWeather weather) {
            super(level, id, weather);
        }
    }

    /** It is about to reach a body for a second. Change the {@link #setShare share} for a part that shields it, or cancel to spare it. */
    public static class Exposed extends SpaceWeatherEvent implements ICancellableEvent {
        private final ServerPlayer player;
        private float share;

        public Exposed(ServerLevel level, ResourceLocation id, SpaceWeather weather, ServerPlayer player, float share) {
            super(level, id, weather);
            this.player = player;
            this.share = share;
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        /** How much of it reaches the body, 0 to 1. */
        public float getShare() {
            return share;
        }

        public void setShare(float share) {
            this.share = Math.max(0f, share);
        }
    }
}
