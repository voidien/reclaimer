package com.voidreclaimer.dust;

import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible cosmic dust clouds of deep space, generated deterministically from a seed.
 *
 * <p>Space is divided into cubic cells of {@link #CELL} blocks. Each cell holds at most one cloud (large and rare),
 * with a random center inside the cell, a radius of {@link #MIN_RADIUS}..{@link #MAX_RADIUS} and a dominant kind.
 * Because a cloud never reaches further than one cell from its own, a lookup only checks the 27 surrounding cells.
 *
 * <p>Worlds created with the starter nebula also get {@link StarterNebula#pockets} near spawn.
 *
 * <p>Generation constants are deliberately not configurable: client (drag prediction, sensor HUD) and server (dust
 * gain, drag) must agree exactly. The client gets the seed through the synced {@link DustFieldSeed} level attachment.
 */
public final class DustField {
    public static final int CELL = 1024;
    public static final double MIN_RADIUS = 150;
    public static final double MAX_RADIUS = 400;
    static final double CLOUD_CHANCE = 0.32;
    /** Noise feature size relative to the cloud radius. */
    private static final double DETAIL_SCALE = 0.3;
    private static final long NOISE_SALT = 0x5D05_7C10_D5EEDL;

    private static volatile DustField cached;

    private final long seed;
    private final SimplexNoise noise;
    @Nullable
    private final Vec3 nebulaCenter;
    private final List<DustCloud> nebula;

    public DustField(long seed) {
        this(seed, null);
    }

    /** @param nebulaCenter center of the {@link StarterNebula}, or null if the world has none */
    public DustField(long seed, @Nullable Vec3 nebulaCenter) {
        this.seed = seed;
        this.noise = new SimplexNoise(RandomSource.create(seed ^ NOISE_SALT));
        this.nebulaCenter = nebulaCenter;
        this.nebula = nebulaCenter == null ? List.of() : StarterNebula.pockets(nebulaCenter);
    }

    /** The field of a deep space level, or null anywhere else (or before the seed has been synced). */
    @Nullable
    public static DustField of(Level level) {
        if (!ModDimensions.isDeepSpace(level)) {
            return null;
        }
        DustFieldSeed fieldSeed = level.getExistingDataOrNull(ModAttachments.DUST_FIELD_SEED);
        if (fieldSeed == null) {
            return null;
        }
        DustField field = cached;
        Vec3 nebula = fieldSeed.nebula().orElse(null);
        if (field == null || field.seed != fieldSeed.seed() || !java.util.Objects.equals(field.nebulaCenter, nebula)) {
            field = new DustField(fieldSeed.seed(), nebula);
            cached = field;
        }
        return field;
    }

    public long seed() {
        return seed;
    }

    /** The cloud generated in a cell, if any. */
    public Optional<DustCloud> cloudInCell(int cx, int cy, int cz) {
        RandomSource random = RandomSource.create(cellSeed(cx, cy, cz));
        if (random.nextDouble() >= CLOUD_CHANCE) {
            return Optional.empty();
        }
        double radius = Mth.lerp(random.nextDouble(), MIN_RADIUS, MAX_RADIUS);
        Vec3 center = new Vec3(
                (cx + random.nextDouble()) * CELL,
                (cy + random.nextDouble()) * CELL,
                (cz + random.nextDouble()) * CELL);
        DustKind kind = DustKind.pick(random.nextDouble());
        double intensity = 0.6 + 0.4 * random.nextDouble();
        Vec3 offset = new Vec3(random.nextDouble() * 10_000, random.nextDouble() * 10_000, random.nextDouble() * 10_000);
        return Optional.of(new DustCloud(center, radius, kind, intensity, offset));
    }

    /** All clouds whose cells lie within {@code range} (plus the maximum radius) of {@code pos}. */
    public List<DustCloud> cloudsAround(Vec3 pos, double range) {
        double reach = range + MAX_RADIUS;
        int minX = Mth.floor((pos.x - reach) / CELL), maxX = Mth.floor((pos.x + reach) / CELL);
        int minY = Mth.floor((pos.y - reach) / CELL), maxY = Mth.floor((pos.y + reach) / CELL);
        int minZ = Mth.floor((pos.z - reach) / CELL), maxZ = Mth.floor((pos.z + reach) / CELL);
        List<DustCloud> clouds = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cloudInCell(x, y, z).ifPresent(clouds::add);
                }
            }
        }
        if (nebulaCenter != null && pos.distanceTo(nebulaCenter) <= reach + StarterNebula.POCKET_SPREAD) {
            clouds.addAll(nebula);
        }
        return clouds;
    }

    /** Density 0..1 of one cloud at a position. */
    public double density(DustCloud cloud, Vec3 pos) {
        double d2 = pos.distanceToSqr(cloud.center()) / (cloud.radius() * cloud.radius());
        if (d2 >= 1.0) {
            return 0.0;
        }
        double falloff = (1.0 - d2) * (1.0 - d2);
        double scale = cloud.radius() * DETAIL_SCALE;
        Vec3 o = cloud.noiseOffset();
        double detail = 0.65 + 0.35 * noise.getValue(pos.x / scale + o.x, pos.y / scale + o.y, pos.z / scale + o.z);
        return Mth.clamp(cloud.intensity() * falloff * detail, 0.0, 1.0);
    }

    /** The densest cloud at a position. Clouds rarely touch; when they do the denser one wins. */
    public Sample sample(Vec3 pos) {
        DustCloud best = null;
        double bestDensity = 0.0;
        for (DustCloud cloud : cloudsAround(pos, 0)) {
            double density = density(cloud, pos);
            if (density > bestDensity) {
                best = cloud;
                bestDensity = density;
            }
        }
        return best == null ? Sample.NONE : new Sample(best, bestDensity);
    }

    /**
     * The cloud whose surface is closest to {@code pos}, within {@code range} blocks of it.
     *
     * @param excludeContaining skip clouds that contain {@code pos} (for "find the next cloud")
     */
    public Optional<DustCloud> nearest(Vec3 pos, double range, boolean excludeContaining) {
        DustCloud best = null;
        double bestDistance = Double.MAX_VALUE;
        for (DustCloud cloud : cloudsAround(pos, range)) {
            if (excludeContaining && cloud.contains(pos)) {
                continue;
            }
            double distance = Math.max(0.0, cloud.surfaceDistance(pos));
            if (distance <= range && distance < bestDistance) {
                best = cloud;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private long cellSeed(int x, int y, int z) {
        long h = seed;
        h = mix(h ^ (x * 0x9E3779B97F4A7C15L));
        h = mix(h ^ (y * 0xC2B2AE3D27D4EB4FL));
        h = mix(h ^ (z * 0x165667B19E3779F9L));
        return h;
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    /** A density reading: the dominant cloud (null when outside every cloud) and its density there. */
    public record Sample(@Nullable DustCloud cloud, double density) {
        public static final Sample NONE = new Sample(null, 0.0);

        public boolean inCloud() {
            return cloud != null && density > 0.0;
        }
    }
}
