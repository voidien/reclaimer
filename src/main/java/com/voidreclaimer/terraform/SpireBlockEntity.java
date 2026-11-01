package com.voidreclaimer.terraform;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.terraform.TerraformProfile;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModBlockEntities;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Terraforming Spire, the controller of its tower ({@link SpireAssembly}), and its Genesis Pulse: a square wave that
 * sweeps out from the tower's centre over {@code duration} ticks, rewriting each ring's blocks as it passes by the
 * {@code void_reclaimer:restored} profile, then rewriting the biome of the whole area and resending its chunks. A pulse
 * in progress is saved with the block and carries on after a reload, whatever happens to the tower. The tower is drawn
 * from this block entity (GeckoLib, {@code geo/entity/spire_tower.geo.json}).
 */
public class SpireBlockEntity extends KineticBlockEntity implements GeoBlockEntity {
    public enum Start {
        STARTED, BUSY, WRONG_WORLD, NOT_ASSEMBLED, NO_PROFILE;

        /** The name scripts and lang keys use. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** How often an assembled tower checks its frame and column. */
    static final int CHECK_TICKS = 20;

    /** Particle points along each ring: the corners and the edge middles. */
    private static final int[][] RING_POINTS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean assembled;
    private int checkIn;
    /** Ticks into the pulse; -1 when idle. */
    private int elapsed = -1;
    private int radius;
    private int duration;
    private int pulses;
    /** Where the running pulse sweeps out from: the tower's centre, or this block for a pulse saved before towers. */
    private BlockPos pulseCentre;
    /** How many chunks the last finished pulse resent. Not saved. */
    private int lastChunksResent;

    public SpireBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        pulseCentre = pos;
    }

    public SpireBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SPIRE.get(), pos, state);
    }

    /** The way the tower stands from the Spire. */
    public Direction facing() {
        return getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    /** Whether the frame was complete and the column filled the last time the tower was assembled. */
    public boolean assembled() {
        return assembled;
    }

    void setAssembled(boolean value) {
        if (assembled == value) {
            return;
        }
        assembled = value;
        checkIn = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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

    /** Blocks from the tower to the edge of the running pulse's area, or of the next one (the config). */
    public int radius() {
        return running() ? radius : Config.SPIRE_RADIUS.get();
    }

    /** Pulses finished here so far. */
    public int pulses() {
        return pulses;
    }

    /** Where the running or last pulse swept out from. */
    public BlockPos pulseCentre() {
        return pulseCentre;
    }

    public int lastChunksResent() {
        return lastChunksResent;
    }

    /** Starts a pulse with the configured size, if the tower stands on ruined Earth and none is running. */
    public Start tryStart() {
        if (level == null || !RuinedEarth.is(level)) {
            return Start.WRONG_WORLD;
        }
        if (running()) {
            return Start.BUSY;
        }
        if (!assembled) {
            return Start.NOT_ASSEMBLED;
        }
        if (profile(level).isEmpty()) {
            return Start.NO_PROFILE;
        }
        start(Config.SPIRE_RADIUS.get(), Config.SPIRE_PULSE_TICKS.get());
        return Start.STARTED;
    }

    /** Starts a pulse of the given size now, whatever the dimension: from the tower's centre, or this block without a tower. */
    public void start(int radius, int duration) {
        this.radius = radius;
        this.duration = duration;
        this.elapsed = 0;
        this.pulseCentre = assembled ? SpireTower.centre(worldPosition, facing()) : worldPosition;
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3f, 0.6f);
        }
    }

    private static Optional<TerraformProfile> profile(Level level) {
        return level.registryAccess().registry(VoidReclaimerRegistries.TERRAFORM_PROFILE)
                .flatMap(registry -> Optional.ofNullable(registry.get(TerraformProfiles.RESTORED)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (assembled && ++checkIn >= CHECK_TICKS) {
            checkIn = 0;
            SpireAssembly.check(this);
        }
        pulseTick(server);
    }

    private void pulseTick(ServerLevel server) {
        if (elapsed < 0) {
            return;
        }
        Optional<TerraformProfile> profile = profile(server);
        if (profile.isEmpty()) {
            elapsed = -1;
            return;
        }
        PulseSchedule schedule = new PulseSchedule(radius, duration);
        elapsed++;
        schedule.forNewRings(elapsed, ring -> {
            PulseSchedule.forRing(ring, (dx, dz) -> Terraforming.swapColumn(server, profile.get(), pulseCentre.getX() + dx, pulseCentre.getZ() + dz));
            for (int[] point : RING_POINTS) {
                int x = pulseCentre.getX() + point[0] * ring;
                int z = pulseCentre.getZ() + point[1] * ring;
                int y = server.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.5, z + 0.5, 3, 0.4, 0.6, 0.4, 0.0);
            }
        });
        if (schedule.isDone(elapsed)) {
            finish(server, profile.get());
        } else if (elapsed % 20 == 0) {
            setChanged();
        }
    }

    private void finish(ServerLevel level, TerraformProfile profile) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(profile.biome());
        lastChunksResent = Terraforming.rewriteBiomes(level, pulseCentre, radius, biome);
        elapsed = -1;
        pulses++;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 3f, 0.5f);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("assembled", assembled);
        tag.putInt("elapsed", elapsed);
        tag.putInt("radius", radius);
        tag.putInt("duration", duration);
        tag.putInt("pulses", pulses);
        tag.putLong("pulseCentre", pulseCentre.asLong());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        assembled = tag.getBoolean("assembled");
        elapsed = tag.contains("elapsed") ? tag.getInt("elapsed") : -1;
        radius = tag.getInt("radius");
        duration = Math.max(1, tag.getInt("duration"));
        pulses = tag.getInt("pulses");
        // A Spire saved before towers has no centre: its pulse carries on from the block itself.
        pulseCentre = tag.contains("pulseCentre") ? BlockPos.of(tag.getLong("pulseCentre")) : worldPosition;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spire", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
