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
        registrar.playToClient(SensorEchoPayload.TYPE, SensorEchoPayload.STREAM_CODEC, SensorEchoPayload::handle);
    }
}
