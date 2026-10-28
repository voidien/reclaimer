package com.voidreclaimer.client.input;

import com.voidreclaimer.ClientConfig;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustSensor;
import com.voidreclaimer.dust.SensorPulse;
import com.voidreclaimer.network.SensorEchoPayload;
import com.voidreclaimer.network.SensorPulsePayload;
import com.voidreclaimer.registry.ModSounds;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client half of the Dust Sensor: the pulse key, the incoming echo, and the directional hum.
 *
 * <p>The hum works only outside clouds and within the hum range of one. Its strength is how directly the player
 * looks at the nearest cloud, scaled by proximity; a soft tone repeats faster and higher as the signal grows.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientSensorController {
    /** How long an echo marker stays on screen. */
    public static final int ECHO_DISPLAY_TICKS = 120;
    private static final int TARGET_REFRESH_TICKS = 10;
    private static final float SIGNAL_SHARPNESS = 6f;

    @Nullable
    private static SensorEchoPayload echo;
    private static int echoAge;
    @Nullable
    private static DustCloud humTarget;
    private static float signal;
    private static int ticksSinceTone;
    private static int tick;

    private ClientSensorController() {}

    /** Installed as {@link SensorEchoPayload#clientHandler}. */
    public static void onEcho(SensorEchoPayload payload) {
        echo = payload;
        echoAge = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && payload.strength() != SensorPulse.Strength.NONE) {
            float volume = 0.4f + 0.2f * payload.strength().ordinal();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SENSOR_ECHO.get(), 1f, volume));
        }
    }

    /** The echo still on screen, or null. */
    @Nullable
    public static SensorEchoPayload echo() {
        return echo;
    }

    /** 0..1 fade of the current echo marker. */
    public static float echoFade() {
        return echo == null ? 0f : 1f - (float) echoAge / ECHO_DISPLAY_TICKS;
    }

    /** Current hum signal 0..1 (0 when inside a cloud, out of range, or without a sensor). */
    public static float signal() {
        return signal;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            echo = null;
            humTarget = null;
            signal = 0f;
            return;
        }
        while (ModKeyMappings.CLOUD_SCANNER.consumeClick()) {
            boolean on = !ClientConfig.CLOUD_SCANNER.get();
            ClientConfig.CLOUD_SCANNER.set(on);
            ClientConfig.CLOUD_SCANNER.save();
            player.displayClientMessage(Component.translatable(on ? "message.void_reclaimer.scanner_on" : "message.void_reclaimer.scanner_off")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        tick++;
        while (ModKeyMappings.SENSOR_PULSE.consumeClick()) {
            PacketDistributor.sendToServer(SensorPulsePayload.INSTANCE);
        }
        if (echo != null && ++echoAge >= ECHO_DISPLAY_TICKS) {
            echo = null;
        }
        updateHum(mc, player);
    }

    private static void updateHum(Minecraft mc, LocalPlayer player) {
        DustField field = DustField.of(player.level());
        if (field == null || !DustSensor.has(player)) {
            humTarget = null;
            signal = 0f;
            return;
        }
        Vec3 eyes = player.getEyePosition();
        double range = Config.HUM_RANGE.get();
        if (tick % TARGET_REFRESH_TICKS == 0 || (humTarget != null && humTarget.contains(eyes))) {
            Optional<DustCloud> nearest = field.nearest(eyes, range, true);
            humTarget = nearest.orElse(null);
        }
        if (humTarget == null || field.sample(eyes).inCloud()) {
            signal = 0f;
            return;
        }
        double distance = Math.max(0.0, humTarget.surfaceDistance(eyes));
        Vec3 toCloud = humTarget.center().subtract(eyes).normalize();
        double alignment = Math.max(0.0, player.getLookAngle().dot(toCloud));
        double proximity = Mth.clamp(1.0 - distance / range, 0.0, 1.0);
        signal = (float) (Math.pow(alignment, SIGNAL_SHARPNESS) * (0.25 + 0.75 * proximity));

        ticksSinceTone++;
        if (signal > 0.05f) {
            int interval = Math.max(4, Math.round(30 - 26 * signal));
            if (ticksSinceTone >= interval) {
                ticksSinceTone = 0;
                float pitch = 0.7f + 0.9f * signal;
                float volume = 0.15f + 0.5f * signal;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SENSOR_HUM.get(), pitch, volume));
            }
        }
    }
}
