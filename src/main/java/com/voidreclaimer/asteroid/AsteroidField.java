package com.voidreclaimer.asteroid;

import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustFieldSeed;
import com.voidreclaimer.dust.DustKinds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Where the asteroids of a world are: a pure function of the world seed. Space is cut into {@value #CELL}-block cells
 * like the dust field; a cell holds nothing (most of them) or one cluster, and no body ever reaches outside its cell, so
 * a chunk only has to ask about its own cell. A cluster is a field of small rocks, or a large body with a few small
 * satellites, or (rarely) a giant. Each cluster is made of the dust kind of the nearest dust cloud, usually.
 */
public final class AsteroidField {
    public static final int CELL = DustField.CELL;
    /** Share of cells that hold a cluster. */
    static final double CELL_CHANCE = 0.35;
    /** Of the cells with a cluster: giants, then large bodies; the rest are small-rock fields. */
    static final double GIANT_CHANCE = 0.04;
    static final double LARGE_CHANCE = 0.16;
    /** Half-width of a giant, before surface noise (diameters up to about 300 blocks). */
    static final double GIANT_MIN = 100;
    static final double GIANT_MAX = 150;
    static final double LARGE_MIN = 18;
    static final double LARGE_MAX = 40;
    static final double SMALL_MIN = 3;
    static final double SMALL_MAX = 8;
    /** How far to look for the dust cloud that decides a cluster's kind, and how often it decides. */
    static final double CLOUD_RANGE = 1500;
    static final double CLOUD_KIND_CHANCE = 0.85;
    private static final long SALT = 0xA57E_201D_5EEDL;
    private static final int CACHE_LIMIT = 4096;

    /** The four built-in kinds an asteroid can be made of, with their weights where no cloud decides. */
    private static final List<ResourceLocation> KINDS = List.of(DustKinds.SILICATE, DustKinds.ICE, DustKinds.CARBON, DustKinds.METALLIC);
    private static final double[] KIND_WEIGHTS = {0.35, 0.2, 0.2, 0.25};

    private final long seed;
    private final DustField dust;
    private final Vec3 spawn;
    private final double safeRadius;
    private final Map<Long, List<AsteroidBody>> cache = new ConcurrentHashMap<>();

    /**
     * @param worldSeed  the world seed
     * @param spawn      the deep space spawn point: nothing generates within {@code safeRadius} of it
     * @param safeRadius clear space around spawn, from the body's nearest edge
     */
    public AsteroidField(long worldSeed, Vec3 spawn, double safeRadius) {
        this.seed = worldSeed ^ SALT;
        this.dust = new DustField(DustFieldSeed.derive(worldSeed));
        this.spawn = spawn;
        this.safeRadius = safeRadius;
    }


    /** The bodies of cell (cx, cy, cz). The same list every time. */
    public List<AsteroidBody> bodiesIn(int cx, int cy, int cz) {
        long key = cellKey(cx, cy, cz);
        List<AsteroidBody> cached = cache.get(key);
        if (cached == null) {
            if (cache.size() > CACHE_LIMIT) {
                cache.clear();
            }
            cached = generate(cx, cy, cz);
            cache.put(key, cached);
        }
        return cached;
    }

    private List<AsteroidBody> generate(int cx, int cy, int cz) {
        RandomSource random = RandomSource.create(mix(seed ^ cellKey(cx, cy, cz)));
        if (random.nextDouble() >= CELL_CHANCE) {
            return List.of();
        }
        double roll = random.nextDouble();
        Vec3 cellOrigin = new Vec3(cx * (double) CELL, cy * (double) CELL, cz * (double) CELL);
        ResourceLocation kind = kindFor(cellOrigin.add(CELL / 2.0, CELL / 2.0, CELL / 2.0), random);
        List<AsteroidBody> bodies = new ArrayList<>();
        if (roll < GIANT_CHANCE) {
            addMain(bodies, random, cellOrigin, kind, AsteroidBody.Size.GIANT, GIANT_MIN, GIANT_MAX);
            addSatellites(bodies, random, cellOrigin, kind, 1 + random.nextInt(3));
        } else if (roll < GIANT_CHANCE + LARGE_CHANCE) {
            addMain(bodies, random, cellOrigin, kind, AsteroidBody.Size.LARGE, LARGE_MIN, LARGE_MAX);
            addSatellites(bodies, random, cellOrigin, kind, 2 + random.nextInt(5));
        } else {
            addRockField(bodies, random, cellOrigin, kind);
        }
        bodies.removeIf(body -> body.center().distanceTo(spawn) < body.maxExtent() + safeRadius);
        return List.copyOf(bodies);
    }

    private void addMain(List<AsteroidBody> bodies, RandomSource random, Vec3 cellOrigin, ResourceLocation kind,
                         AsteroidBody.Size size, double min, double max) {
        double base = Mth.lerp(random.nextDouble(), min, max);
        double rx = base * Mth.lerp(random.nextDouble(), 0.7, 1.0);
        double ry = base * Mth.lerp(random.nextDouble(), 0.7, 1.0);
        double rz = base * Mth.lerp(random.nextDouble(), 0.7, 1.0);
        double extent = Math.max(rx, Math.max(ry, rz)) * AsteroidBody.OUTER;
        Vec3 center = new Vec3(
                cellOrigin.x + inside(random, extent),
                cellOrigin.y + inside(random, extent),
                cellOrigin.z + inside(random, extent));
        bodies.add(new AsteroidBody(center, rx, ry, rz, kind, random.nextLong(), size));
    }

    /** Small rocks around the main body, in the room left inside the cell. */
    private void addSatellites(List<AsteroidBody> bodies, RandomSource random, Vec3 cellOrigin, ResourceLocation kind, int count) {
        AsteroidBody main = bodies.get(0);
        for (int i = 0; i < count; i++) {
            double r = Mth.lerp(random.nextDouble(), SMALL_MIN, SMALL_MAX);
            double extent = r * 1.3 * AsteroidBody.OUTER;
            double distance = main.maxExtent() + extent + Mth.lerp(random.nextDouble(), 5, 40);
            Vec3 direction = randomDirection(random);
            Vec3 center = main.center().add(direction.scale(distance));
            if (fits(center, extent, cellOrigin) && !touchesAny(bodies, center, extent)) {
                bodies.add(small(random, center, r, kind));
            }
        }
    }

    /** A loose field of small rocks around a point of the cell. */
    private void addRockField(List<AsteroidBody> bodies, RandomSource random, Vec3 cellOrigin, ResourceLocation kind) {
        double spread = Mth.lerp(random.nextDouble(), 40, 110);
        double margin = spread + SMALL_MAX * 1.3 * AsteroidBody.OUTER;
        Vec3 middle = new Vec3(
                cellOrigin.x + inside(random, margin),
                cellOrigin.y + inside(random, margin),
                cellOrigin.z + inside(random, margin));
        int count = 4 + random.nextInt(11);
        for (int i = 0; i < count; i++) {
            double r = Mth.lerp(random.nextDouble(), SMALL_MIN, SMALL_MAX);
            double extent = r * 1.3 * AsteroidBody.OUTER;
            Vec3 center = middle.add(randomDirection(random).scale(spread * Math.cbrt(random.nextDouble())));
            if (fits(center, extent, cellOrigin) && !touchesAny(bodies, center, extent)) {
                bodies.add(small(random, center, r, kind));
            }
        }
    }

    private static AsteroidBody small(RandomSource random, Vec3 center, double r, ResourceLocation kind) {
        return new AsteroidBody(center, r, r * Mth.lerp(random.nextDouble(), 0.7, 1.0), r * Mth.lerp(random.nextDouble(), 0.7, 1.0),
                kind, random.nextLong(), AsteroidBody.Size.SMALL);
    }

    private static boolean fits(Vec3 center, double extent, Vec3 cellOrigin) {
        return center.x - extent >= cellOrigin.x && center.x + extent <= cellOrigin.x + CELL
                && center.y - extent >= cellOrigin.y && center.y + extent <= cellOrigin.y + CELL
                && center.z - extent >= cellOrigin.z && center.z + extent <= cellOrigin.z + CELL;
    }

    private static boolean touchesAny(List<AsteroidBody> bodies, Vec3 center, double extent) {
        for (AsteroidBody other : bodies) {
            if (other.center().distanceTo(center) < other.maxExtent() + extent) {
                return true;
            }
        }
        return false;
    }

    /** A coordinate inside the cell that keeps {@code margin} blocks to both walls. */
    private static double inside(RandomSource random, double margin) {
        return Mth.lerp(random.nextDouble(), margin, CELL - margin);
    }

    private static Vec3 randomDirection(RandomSource random) {
        double y = random.nextDouble() * 2 - 1;
        double angle = random.nextDouble() * Math.PI * 2;
        double ring = Math.sqrt(1 - y * y);
        return new Vec3(ring * Math.cos(angle), y, ring * Math.sin(angle));
    }

    /** The dust kind of a cluster centered at {@code pos}: usually that of the nearest cloud, else by weight. */
    ResourceLocation kindFor(Vec3 pos, RandomSource random) {
        Optional<DustCloud> cloud = dust.nearest(pos, CLOUD_RANGE, false);
        double chance = random.nextDouble();
        if (cloud.isPresent() && KINDS.contains(cloud.get().kind()) && chance < CLOUD_KIND_CHANCE) {
            return cloud.get().kind();
        }
        double pick = random.nextDouble();
        for (int i = 0; i < KINDS.size(); i++) {
            pick -= KIND_WEIGHTS[i];
            if (pick < 0) {
                return KINDS.get(i);
            }
        }
        return KINDS.get(0);
    }

    public Vec3 spawn() {
        return spawn;
    }

    /** The dust field the kinds are chosen from (tests). */
    public DustField dust() {
        return dust;
    }

    /** An exact key: 21 bits per axis, far more than any world border needs (cells are 1024 blocks). */
    private static long cellKey(int cx, int cy, int cz) {
        return ((long) (cx & 0x1FFFFF) << 42) | ((long) (cy & 0x1FFFFF) << 21) | (cz & 0x1FFFFF);
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }
}
