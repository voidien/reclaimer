package com.voidreclaimer.dust;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.network.SensorEchoPayload;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModSounds;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Dust Sensor pulse: a deliberate, costly ping that answers "roughly which way is the nearest cloud?" and
 * nothing more. The echo returns after a delay that grows with distance, points in a direction blurred by an error
 * that grows with distance, and only says how strong it was. Finding the cloud is up to the player (pulse again,
 * triangulate, then follow the hum).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class SensorPulse {
    private static final double MIN_ERROR_DEGREES = 6.0;
    private static final double MAX_EXTRA_ERROR_DEGREES = 24.0;
    private static final int MIN_DELAY_TICKS = 20;
    private static final double BLOCKS_PER_DELAY_TICK = 40.0;

    private SensorPulse() {}

    public enum Strength {
        NONE, FAINT, MODERATE, STRONG;

        static Strength forDistance(double distance, double range) {
            double fraction = distance / range;
            if (fraction < 0.25) {
                return STRONG;
            }
            return fraction < 0.6 ? MODERATE : FAINT;
        }
    }

    /** An echo: a blurred unit direction (zero for NONE), its strength and its travel time. */
    public record Echo(Vec3 direction, Strength strength, int delayTicks) {}

    /** Result of a pulse request. */
    public enum Result { SENT, NO_SENSOR, COOLING_DOWN, LOW_POWER }

    /** Handles a pulse request from a player. */
    public static Result pulse(ServerPlayer player) {
        if (!DustSensor.has(player)) {
            return reply(player, Result.NO_SENSOR, "message.void_reclaimer.sensor_missing");
        }
        SensorState state = player.getData(ModAttachments.SENSOR_STATE);
        long now = player.level().getGameTime();
        if (now < state.nextPulseTick) {
            return reply(player, Result.COOLING_DOWN, "message.void_reclaimer.sensor_cooldown");
        }
        if (!Energy.canPowerSystems(player)) {
            return reply(player, Result.LOW_POWER, "message.void_reclaimer.sensor_low_power");
        }
        Energy.consume(player, Config.PULSE_ENERGY_COST.get().floatValue());
        state.nextPulseTick = now + Config.PULSE_COOLDOWN_SECONDS.get() * 20L;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SENSOR_PULSE.get(), SoundSource.PLAYERS, 1f, 1f);

        DustField field = DustField.of(player.level());
        double range = Config.PULSE_RANGE.get();
        Echo echo = field == null
                ? new Echo(Vec3.ZERO, Strength.NONE, delayFor(range))
                : computeEcho(field, player.getEyePosition(), range, player.getRandom());
        state.pendingEcho = echo;
        state.echoDueTick = now + echo.delayTicks();
        return Result.SENT;
    }

    /** Pure echo computation, used by the pulse and by tests. */
    public static Echo computeEcho(DustField field, Vec3 origin, double range, RandomSource random) {
        Optional<DustCloud> target = field.nearest(origin, range, true);
        if (target.isEmpty()) {
            return new Echo(Vec3.ZERO, Strength.NONE, delayFor(range));
        }
        DustCloud cloud = target.get();
        double distance = Math.max(0.0, cloud.surfaceDistance(origin));
        Vec3 exact = cloud.center().subtract(origin).normalize();
        double maxError = Math.toRadians(MIN_ERROR_DEGREES + MAX_EXTRA_ERROR_DEGREES * Mth.clamp(distance / range, 0.0, 1.0));
        return new Echo(blur(exact, maxError, random), Strength.forDistance(distance, range), delayFor(distance));
    }

    /** Rotates {@code direction} by a random angle up to {@code maxError} radians around a random perpendicular axis. */
    static Vec3 blur(Vec3 direction, double maxError, RandomSource random) {
        Vector3f dir = direction.toVector3f();
        Vector3f helper = Math.abs(dir.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f axis = new Vector3f(dir).cross(helper).normalize();
        axis.rotateAxis((float) (random.nextDouble() * Math.PI * 2), dir.x, dir.y, dir.z);
        float angle = (float) (random.nextDouble() * maxError);
        Vector3f blurred = new Vector3f(dir).rotate(new Quaternionf().rotationAxis(angle, axis));
        return new Vec3(blurred).normalize();
    }

    static int delayFor(double distance) {
        return MIN_DELAY_TICKS + (int) (distance / BLOCKS_PER_DELAY_TICK);
    }

    /** Delivers echoes when they arrive. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SensorState state = player.getData(ModAttachments.SENSOR_STATE);
        if (state.pendingEcho != null && player.level().getGameTime() >= state.echoDueTick) {
            Echo echo = state.pendingEcho;
            state.pendingEcho = null;
            PacketDistributor.sendToPlayer(player, new SensorEchoPayload(echo.direction(), echo.strength()));
        }
    }

    private static Result reply(ServerPlayer player, Result result, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
        return result;
    }
}
