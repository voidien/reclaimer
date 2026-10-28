package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.FlightSettings;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: flip one flight toggle. The server owns the settings and syncs them back. */
public record ToggleFlightSettingPayload(Setting setting) implements CustomPacketPayload {
    public static final Type<ToggleFlightSettingPayload> TYPE = new Type<>(VoidReclaimer.id("toggle_flight_setting"));
    public static final StreamCodec<ByteBuf, ToggleFlightSettingPayload> STREAM_CODEC =
            Setting.STREAM_CODEC.map(ToggleFlightSettingPayload::new, ToggleFlightSettingPayload::setting);

    public enum Setting {
        DAMPENER,
        CREATIVE_THRUSTERS;

        private static final IntFunction<Setting> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Setting> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    static void handle(ToggleFlightSettingPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        FlightSettings settings = player.getData(ModAttachments.FLIGHT_SETTINGS);
        switch (payload.setting()) {
            case DAMPENER -> {
                settings = settings.withDampener(!settings.dampener());
                player.displayClientMessage(Component.translatable(settings.dampener()
                        ? "message.void_reclaimer.dampener_on" : "message.void_reclaimer.dampener_off").withStyle(ChatFormatting.AQUA), true);
            }
            case CREATIVE_THRUSTERS -> {
                if (!player.isCreative()) {
                    player.displayClientMessage(Component.translatable("message.void_reclaimer.creative_only"), true);
                    return;
                }
                settings = settings.withCreativeThrusters(!settings.creativeThrusters());
                if (settings.creativeThrusters() && player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                player.displayClientMessage(Component.translatable(settings.creativeThrusters()
                        ? "message.void_reclaimer.creative_thrusters_on" : "message.void_reclaimer.creative_thrusters_off").withStyle(ChatFormatting.AQUA), true);
            }
        }
        player.setData(ModAttachments.FLIGHT_SETTINGS, settings);
        player.syncData(ModAttachments.FLIGHT_SETTINGS);
    }
}
