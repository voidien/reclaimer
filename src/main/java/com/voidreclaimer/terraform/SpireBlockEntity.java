package com.voidreclaimer.terraform;

import com.voidreclaimer.Config;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.terraform.TerraformProfile;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A Terraforming Spire and its Genesis Pulse: a square wave that sweeps out from the Spire over {@code duration}
 * ticks, rewriting each ring's blocks as it passes by the {@code void_reclaimer:restored} profile, then rewriting the
 * biome of the whole area and resending its chunks. A pulse in progress is saved with the block and carries on after
 * a reload.
 */
public final class SpireBlockEntity extends BlockEntity {
    public enum Start { STARTED, BUSY, WRONG_WORLD, NO_PROFILE }

    /** Particle points along each ring: the corners and the edge middles. */
    private static final int[][] RING_POINTS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    /** Ticks into the pulse; -1 when idle. */
    private int elapsed = -1;
    private int radius;
    private int duration;
    private int pulses;
    /** How many chunks the last finished pulse resent. Not saved. */
    private int lastChunksResent;

    public SpireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRE.get(), pos, state);
    }

    public boolean running() {
        return elapsed >= 0;
    }

    /** Ticks into the running pulse, -1 when idle. */
    public int elapsed() {
        return elapsed;
    }

    /** How far the running pulse has swept, 0 to 1; 0 when idle. */
    public float progress() {
        return running() ? Math.min(1f, elapsed / (float) Math.max(1, duration)) : 0f;
    }

    /** Blocks from the Spire to the edge of the running pulse's area, or of the next one (the config). */
    public int radius() {
        return running() ? radius : Config.SPIRE_RADIUS.get();
    }

    /** Pulses finished here so far. */
    public int pulses() {
        return pulses;
    }

    public int lastChunksResent() {
        return lastChunksResent;
    }

    /** Starts a pulse with the configured size, if this is ruined Earth and none is running. */
    public Start tryStart() {
        if (level == null || !RuinedEarth.is(level)) {
            return Start.WRONG_WORLD;
        }
        if (running()) {
            return Start.BUSY;
        }
        if (profile(level).isEmpty()) {
            return Start.NO_PROFILE;
        }
        start(Config.SPIRE_RADIUS.get(), Config.SPIRE_PULSE_TICKS.get());
        return Start.STARTED;
    }

    /** Starts a pulse of the given size now, whatever the dimension. */
    public void start(int radius, int duration) {
        this.radius = radius;
        this.duration = duration;
        this.elapsed = 0;
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3f, 0.6f);
        }
    }

    private static Optional<TerraformProfile> profile(Level level) {
        return level.registryAccess().registry(VoidReclaimerRegistries.TERRAFORM_PROFILE)
                .flatMap(registry -> Optional.ofNullable(registry.get(TerraformProfiles.RESTORED)));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpireBlockEntity spire) {
        if (spire.elapsed < 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Optional<TerraformProfile> profile = profile(level);
        if (profile.isEmpty()) {
            spire.elapsed = -1;
            return;
        }
        PulseSchedule schedule = new PulseSchedule(spire.radius, spire.duration);
        spire.elapsed++;
        schedule.forNewRings(spire.elapsed, ring -> {
            PulseSchedule.forRing(ring, (dx, dz) -> Terraforming.swapColumn(serverLevel, profile.get(), pos.getX() + dx, pos.getZ() + dz));
            for (int[] point : RING_POINTS) {
                int x = pos.getX() + point[0] * ring;
                int z = pos.getZ() + point[1] * ring;
                int y = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 3, 0.4, 0.6, 0.4, 0.0);
            }
        });
        if (schedule.isDone(spire.elapsed)) {
            spire.finish(serverLevel, profile.get());
        } else if (spire.elapsed % 20 == 0) {
            spire.setChanged();
        }
    }

    private void finish(ServerLevel level, TerraformProfile profile) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(profile.biome());
        lastChunksResent = Terraforming.rewriteBiomes(level, worldPosition, radius, biome);
        elapsed = -1;
        pulses++;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 3f, 0.5f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("elapsed", elapsed);
        tag.putInt("radius", radius);
        tag.putInt("duration", duration);
        tag.putInt("pulses", pulses);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        elapsed = tag.contains("elapsed") ? tag.getInt("elapsed") : -1;
        radius = tag.getInt("radius");
        duration = Math.max(1, tag.getInt("duration"));
        pulses = tag.getInt("pulses");
    }
}
