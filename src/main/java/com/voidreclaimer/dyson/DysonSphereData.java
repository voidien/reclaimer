package com.voidreclaimer.dyson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * World-wide Dyson Sphere progress, stored as a saved, synced attachment on the deep space level.
 * Satellites drive skybox tiers 1-2 and shell truss modules drive tiers 3-4; the two counters are kept apart.
 *
 * <p>{@code latticeStart} and {@code shellTotal} are the server's thresholds at the time of the last write, carried
 * along so clients can draw the shell without reading a config the server owns.
 *
 * @param satellites  Dyson Swarm Satellite Payloads launched
 * @param trusses     Dyson Shell Truss Modules launched
 * @param latticeStart trusses needed for tier 3
 * @param shellTotal   trusses that complete the shell (tier 4)
 */
public record DysonSphereData(long satellites, long trusses, long latticeStart, long shellTotal) {
    public static final Codec<DysonSphereData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("satellites").forGetter(DysonSphereData::satellites),
            Codec.LONG.fieldOf("trusses").forGetter(DysonSphereData::trusses),
            Codec.LONG.fieldOf("lattice_start").forGetter(DysonSphereData::latticeStart),
            Codec.LONG.fieldOf("shell_total").forGetter(DysonSphereData::shellTotal)
    ).apply(instance, DysonSphereData::new));

    public static final StreamCodec<ByteBuf, DysonSphereData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DysonSphereData::satellites,
            ByteBufCodecs.VAR_LONG, DysonSphereData::trusses,
            ByteBufCodecs.VAR_LONG, DysonSphereData::latticeStart,
            ByteBufCodecs.VAR_LONG, DysonSphereData::shellTotal,
            DysonSphereData::new);

    public DysonSphereData {
        satellites = Math.max(0, satellites);
        trusses = Math.max(0, trusses);
        latticeStart = Math.max(1, latticeStart);
        shellTotal = Math.max(latticeStart, shellTotal);
    }

    /** Nothing launched yet, with the thresholds from the config. */
    public static DysonSphereData createDefault() {
        return new DysonSphereData(0, 0, DysonTiers.DEFAULT_LATTICE_START, DysonTiers.DEFAULT_SHELL_TOTAL);
    }

    public int tier() {
        return DysonTiers.tier(satellites, trusses, latticeStart, shellTotal);
    }

    /** 0 to 1: how dense the swarm is drawn. */
    public float swarmDensity() {
        return DysonTiers.swarmDensity(satellites);
    }

    /** 0 to 1: how much of the shell is built. */
    public float shellProgress() {
        return DysonTiers.shellProgress(trusses, shellTotal);
    }

    /** 1 down to 0: how much of the star's light still reaches us. */
    public float sunlight() {
        return DysonTiers.sunlight(tier(), shellProgress());
    }

    public DysonSphereData withSatellites(long value) {
        return new DysonSphereData(value, trusses, latticeStart, shellTotal);
    }

    public DysonSphereData withTrusses(long value) {
        return new DysonSphereData(satellites, value, latticeStart, shellTotal);
    }

    public DysonSphereData withThresholds(long newLatticeStart, long newShellTotal) {
        return new DysonSphereData(satellites, trusses, newLatticeStart, newShellTotal);
    }

    /**
     * The sphere's data as seen from {@code level}: a client in deep space sees the synced attachment, the server
     * reads it from its deep space level whichever dimension asks. {@link #createDefault} when there is none yet.
     */
    public static DysonSphereData of(Level level) {
        Level holder = level;
        if (level instanceof ServerLevel serverLevel && !ModDimensions.isDeepSpace(level)) {
            holder = serverLevel.getServer().getLevel(ModDimensions.DEEP_SPACE);
        }
        DysonSphereData data = holder == null ? null : holder.getExistingDataOrNull(ModAttachments.DYSON_SPHERE);
        return data == null ? createDefault() : data;
    }

    public static DysonSphereData of(MinecraftServer server) {
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        return space == null ? createDefault() : of(space);
    }
}
