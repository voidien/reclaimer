package com.voidreclaimer.weather;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.weather.WeatherEffectType;
import com.voidreclaimer.api.weather.WeatherExposureType;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Wear;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.energy.EnergyTicker;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The exposures and effects the mod's own weather is made of. */
public final class BuiltinWeather {
    public static final DeferredRegister<WeatherExposureType> EXPOSURES = DeferredRegister.create(VoidReclaimerRegistries.WEATHER_EXPOSURE_TYPE, VoidReclaimer.MOD_ID);
    public static final DeferredRegister<WeatherEffectType> EFFECTS = DeferredRegister.create(VoidReclaimerRegistries.WEATHER_EFFECT_TYPE, VoidReclaimer.MOD_ID);
    /** How far a body looks towards the sun for cover, in blocks. */
    private static final double COVER_REACH = 96.0;

    static {
        EXPOSURES.register("everywhere", () -> player -> 1f);
        EXPOSURES.register("open_sky", () -> player -> EnergyTicker.sunExposure(player) > 0f ? 1f : 0f);
        EXPOSURES.register("sun_line", () -> player -> sunLine(player) ? 1f : 0f);
        EFFECTS.register("wear", () -> BuiltinWeather::wear);
        EFFECTS.register("damage", () -> BuiltinWeather::damage);
        EFFECTS.register("energy", () -> BuiltinWeather::energy);
    }

    private BuiltinWeather() {}

    /** Where the sun is from {@code level}, as a direction. Sable carries a clip through ships' hulls too. */
    public static Vec3 toSun(Level level) {
        double angle = level.getSunAngle(1f);
        return new Vec3(-Math.sin(angle), Math.cos(angle), 0.0);
    }

    private static boolean sunLine(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition();
        ClipContext look = new ClipContext(eyes, eyes.add(toSun(player.level()).scale(COVER_REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        return player.level().clip(look).getType() == HitResult.Type.MISS;
    }

    /** {@code amount} of wear on every section, each second. It never breaks one. */
    private static void wear(ServerPlayer player, float amount) {
        for (String socket : Bodies.data(player).sections().keySet()) {
            Wear.add(player, socket, amount);
        }
    }

    /** {@code amount} is a share of the body's health each second. */
    private static void damage(ServerPlayer player, float amount) {
        player.hurt(player.serverLevel().damageSources().magic(), amount * player.getMaxHealth());
    }

    /** {@code amount} is a share of the body's energy store each second. */
    private static void energy(ServerPlayer player, float amount) {
        EnergyData energy = player.getData(ModAttachments.ENERGY);
        Energy.update(player, new EnergyData(Math.max(0f, energy.energy() - amount * energy.capacity()), energy.capacity(), energy.lowPower()), true);
    }
}
