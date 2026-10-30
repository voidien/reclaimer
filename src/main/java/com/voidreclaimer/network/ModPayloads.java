package com.voidreclaimer.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers all custom payloads. Handlers run on the main thread (the registrar's default). */
public final class ModPayloads {
    private static final String PROTOCOL_VERSION = "1";

    private ModPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ThrustInputPayload.TYPE, ThrustInputPayload.STREAM_CODEC, ThrustInputPayload::handle);
        registrar.playToServer(ToggleFlightSettingPayload.TYPE, ToggleFlightSettingPayload.STREAM_CODEC, ToggleFlightSettingPayload::handle);
        registrar.playToServer(SensorPulsePayload.TYPE, SensorPulsePayload.STREAM_CODEC, SensorPulsePayload::handle);
        registrar.playToServer(ReplicatorActionPayload.TYPE, ReplicatorActionPayload.STREAM_CODEC, ReplicatorActionPayload::handle);
        registrar.playToServer(OpenInventoryPayload.TYPE, OpenInventoryPayload.STREAM_CODEC, OpenInventoryPayload::handle);
        registrar.playToServer(NaniteRangePayload.TYPE, NaniteRangePayload.STREAM_CODEC, NaniteRangePayload::handle);
        registrar.playToServer(TetherRecallPayload.TYPE, TetherRecallPayload.STREAM_CODEC, TetherRecallPayload::handle);
        registrar.playToServer(CoreActionPayload.TYPE, CoreActionPayload.STREAM_CODEC, CoreActionPayload::handle);
        registrar.playToServer(RespawnChoicePayload.TYPE, RespawnChoicePayload.STREAM_CODEC, RespawnChoicePayload::handle);
        registrar.playToServer(BodyActionPayload.TYPE, BodyActionPayload.STREAM_CODEC, BodyActionPayload::handle);
        registrar.playToClient(LaunchStreakPayload.TYPE, LaunchStreakPayload.STREAM_CODEC, LaunchStreakPayload::handle);
        registrar.playToClient(SensorEchoPayload.TYPE, SensorEchoPayload.STREAM_CODEC, SensorEchoPayload::handle);
    }
}
