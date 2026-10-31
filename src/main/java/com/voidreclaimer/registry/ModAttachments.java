package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.core.BackupCoreList;
import com.voidreclaimer.core.DeathMarker;
import com.voidreclaimer.tether.RecallState;
import com.voidreclaimer.tether.TetherData;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustFieldSeed;
import com.voidreclaimer.dust.SensorState;
import com.voidreclaimer.dust.StarterNebula;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.flight.FlightSettings;
import com.voidreclaimer.flight.FlightTracker;
import com.voidreclaimer.replicator.ReplicatorData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, VoidReclaimer.MOD_ID);

    /** Per-player energy. Persisted, kept on death, and synced only to the owning player. */
    public static final Supplier<AttachmentType<EnergyData>> ENERGY = ATTACHMENT_TYPES.register("energy",
            () -> AttachmentType.builder(EnergyData::createDefault)
                    .serialize(EnergyData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, EnergyData.STREAM_CODEC)
                    .build());

    /** Per-player flight toggles. Persisted, kept on death, synced to the owning player. */
    public static final Supplier<AttachmentType<FlightSettings>> FLIGHT_SETTINGS = ATTACHMENT_TYPES.register("flight_settings",
            () -> AttachmentType.builder(() -> FlightSettings.DEFAULT)
                    .serialize(FlightSettings.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, FlightSettings.STREAM_CODEC)
                    .build());

    /**
     * Thruster keys currently held. Transient: the client sets it locally every tick and sends changes to the server
     * with {@link com.voidreclaimer.network.ThrustInputPayload}.
     */
    public static final Supplier<AttachmentType<FlightInput>> FLIGHT_INPUT = ATTACHMENT_TYPES.register("flight_input",
            () -> AttachmentType.builder(() -> FlightInput.NONE).build());

    /** Server-only, transient: recent positions for the speed check. */
    public static final Supplier<AttachmentType<FlightTracker>> FLIGHT_TRACKER = ATTACHMENT_TYPES.register("flight_tracker",
            () -> AttachmentType.builder(FlightTracker::new).build());

    /**
     * Dust the player has actually gathered (from clouds and by drilling), per kind, over the whole game. Milestones count this,
     * not the reserve, so a gift of starter dust or a refund never reaches them. Persisted, kept on death, server only.
     */
    public static final Supplier<AttachmentType<DustData>> DUST_COLLECTED = ATTACHMENT_TYPES.register("dust_collected",
            () -> AttachmentType.builder(() -> DustData.EMPTY)
                    .serialize(DustData.CODEC)
                    .copyOnDeath()
                    .build());

    /** Per-player dust reserve. Persisted, kept on death, synced to the owning player. */
    public static final Supplier<AttachmentType<DustData>> DUST = ATTACHMENT_TYPES.register("dust",
            () -> AttachmentType.builder(() -> DustData.EMPTY)
                    .serialize(DustData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, DustData.STREAM_CODEC)
                    .build());

    /** Deep space level only: the dust field seed, synced to every client in that level. Recomputed on load. */
    public static final Supplier<AttachmentType<DustFieldSeed>> DUST_FIELD_SEED = ATTACHMENT_TYPES.register("dust_field_seed",
            () -> AttachmentType.builder(() -> new DustFieldSeed(0L))
                    .sync(DustFieldSeed.STREAM_CODEC)
                    .build());

    /** Deep space level only: whether this world has the starter nebula, fixed on first load. Saved, server-only. */
    public static final Supplier<AttachmentType<StarterNebula.Locked>> STARTER_NEBULA = ATTACHMENT_TYPES.register("starter_nebula",
            () -> AttachmentType.builder(() -> new StarterNebula.Locked(false))
                    .serialize(StarterNebula.Locked.CODEC)
                    .build());

    /** Deep space level only: Dyson Sphere progress. Saved with the level and synced to every client in it. */
    public static final Supplier<AttachmentType<DysonSphereData>> DYSON_SPHERE = ATTACHMENT_TYPES.register("dyson_sphere",
            () -> AttachmentType.builder(DysonSphereData::createDefault)
                    .serialize(DysonSphereData.CODEC)
                    .sync(DysonSphereData.STREAM_CODEC)
                    .build());

    /** Any level: its stations (names and world positions), refreshed by {@code Stations}; synced to every client in it. Not saved: rebuilt on load. */
    public static final Supplier<AttachmentType<com.voidreclaimer.station.StationList>> STATIONS = ATTACHMENT_TYPES.register("stations",
            () -> AttachmentType.builder(() -> com.voidreclaimer.station.StationList.EMPTY)
                    .sync(com.voidreclaimer.station.StationList.STREAM_CODEC)
                    .build());

    /** Ruined Earth level only: which of the three skies is shown. Saved with the level and synced to every client in it. */
    public static final Supplier<AttachmentType<com.voidreclaimer.earth.SkyEra>> EARTH_SKY_ERA = ATTACHMENT_TYPES.register("earth_sky_era",
            () -> AttachmentType.builder(() -> com.voidreclaimer.earth.SkyEra.RUINED)
                    .serialize(com.voidreclaimer.earth.SkyEra.CODEC)
                    .sync(com.voidreclaimer.earth.SkyEra.STREAM_CODEC)
                    .build());

    /** Ruined Earth level only: the return gate and where the way back leaves. Saved with the level, not synced. */
    public static final Supplier<AttachmentType<com.voidreclaimer.earth.PortalLinks>> PORTAL_LINKS = ATTACHMENT_TYPES.register("portal_links",
            () -> AttachmentType.builder(() -> com.voidreclaimer.earth.PortalLinks.NONE)
                    .serialize(com.voidreclaimer.earth.PortalLinks.CODEC)
                    .build());

    /** Server-only, transient: the keys a seated pilot holds (sent by the client when they change). */
    public static final Supplier<AttachmentType<com.voidreclaimer.ship.PilotInput>> PILOT_INPUT = ATTACHMENT_TYPES.register("pilot_input",
            () -> AttachmentType.builder(() -> com.voidreclaimer.ship.PilotInput.NONE).build());

    /** Server-only, transient: sensor pulse cooldown and the pending echo. */
    public static final Supplier<AttachmentType<SensorState>> SENSOR_STATE = ATTACHMENT_TYPES.register("sensor_state",
            () -> AttachmentType.builder(SensorState::new).build());

    /** Per-player Replicator queue, output buffer and milestones. Persisted, kept on death, synced to the owner. */
    public static final Supplier<AttachmentType<ReplicatorData>> REPLICATOR = ATTACHMENT_TYPES.register("replicator",
            () -> AttachmentType.builder(() -> ReplicatorData.EMPTY)
                    .serialize(ReplicatorData.CODEC)
                    .copyOnDeath()
                    .sync(new AttachmentSyncHandler<ReplicatorData>() {
                        @Override
                        public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
                            return holder == to;
                        }

                        @Override
                        public void write(RegistryFriendlyByteBuf buf, ReplicatorData data, boolean initialSync) {
                            ReplicatorData.STREAM_CODEC.encode(buf, data);
                        }

                        @Override
                        public ReplicatorData read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, ReplicatorData previous) {
                            ReplicatorData data = ReplicatorData.STREAM_CODEC.decode(buf);
                            ReplicatorData.clientSyncListener.accept(previous, data);
                            return data;
                        }
                    })
                    .build());

    /** Per-player tether: anchor, clip and recall state. Persisted, kept on death, synced to the owner. */
    public static final Supplier<AttachmentType<TetherData>> TETHER = ATTACHMENT_TYPES.register("tether",
            () -> AttachmentType.builder(() -> TetherData.NONE)
                    .serialize(TetherData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, TetherData.STREAM_CODEC)
                    .build());

    /** Server-only, transient: recall key hold. */
    public static final Supplier<AttachmentType<RecallState>> RECALL_STATE = ATTACHMENT_TYPES.register("recall_state",
            () -> AttachmentType.builder(RecallState::new).build());

    /** Per-player Backup Cores and the death screen choice. Persisted, kept on death, synced to the owner. */
    public static final Supplier<AttachmentType<BackupCoreList>> BACKUP_CORES = ATTACHMENT_TYPES.register("backup_cores",
            () -> AttachmentType.builder(() -> BackupCoreList.EMPTY)
                    .serialize(BackupCoreList.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, BackupCoreList.STREAM_CODEC)
                    .build());

    /** Per-player last death position, for the HUD marker. Persisted, kept on death, synced to the owner. */
    public static final Supplier<AttachmentType<DeathMarker>> DEATH_MARKER = ATTACHMENT_TYPES.register("death_marker",
            () -> AttachmentType.builder(() -> DeathMarker.NONE)
                    .serialize(DeathMarker.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, DeathMarker.STREAM_CODEC)
                    .build());

    /** Per-player modular body. Persisted, kept on death, synced to the owner and to players who see them. Defaults to the starter drone. */
    public static final Supplier<AttachmentType<BodyData>> BODY = ATTACHMENT_TYPES.register("body",
            () -> AttachmentType.builder(Bodies::starter)
                    .serialize(BodyData.CODEC)
                    .copyOnDeath()
                    // Everyone who can see the player gets the body, to draw it on the model.
                    .sync((holder, to) -> true, BodyData.STREAM_CODEC)
                    .build());

    private ModAttachments() {}
}
