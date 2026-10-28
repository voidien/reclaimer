package com.voidreclaimer.dust;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * Optional starter nebula: a guaranteed cluster of small clouds, one of each dust kind, a short flight from spawn.
 *
 * <p>It is chosen per world with the {@link #RULE} game rule (off by default) when the world is created. The deep
 * space level reads the rule once on its first load and stores the answer ({@link Locked}), so flipping the rule later
 * never makes clouds appear or vanish. The server computes the nebula center and sends it to clients inside
 * {@link DustFieldSeed}, so both sides always generate the same clouds.
 */
public final class StarterNebula {
    /** Game rule: generate the starter nebula in new worlds. Only read when the world is first created. */
    public static final GameRules.Key<GameRules.BooleanValue> RULE =
            GameRules.register("voidReclaimerStarterNebula", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(false));

    /** Distance from spawn to the nebula center. */
    public static final double DISTANCE = 450;
    /** Radius of each kind's pocket. */
    public static final double POCKET_RADIUS = 110;
    /** Distance from the nebula center to each pocket's center. */
    static final double POCKET_SPREAD = 60;
    private static final double POCKET_INTENSITY = 0.85;
    private static final long DIRECTION_SALT = 0x5747_7E4E_B01AL;

    private StarterNebula() {}

    /** Registers {@link #RULE}: called during mod construction so the rule exists before any world is created. */
    public static void init() {}

    /**
     * Where the nebula sits: {@link #DISTANCE} blocks from spawn, at spawn height, in a horizontal direction that
     * depends on the dust field seed.
     */
    public static Vec3 center(long fieldSeed, Vec3 spawn) {
        long h = fieldSeed ^ DIRECTION_SALT;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        double angle = (h >>> 11) * 0x1.0p-53 * Mth.TWO_PI;
        return spawn.add(Math.cos(angle) * DISTANCE, 0, Math.sin(angle) * DISTANCE);
    }

    /** The four pockets around {@code center}: metallic, silicate, ice and carbon, at tetrahedron corners. */
    public static List<DustCloud> pockets(Vec3 center) {
        double d = POCKET_SPREAD / Math.sqrt(3);
        Vec3[] offsets = {new Vec3(d, d, d), new Vec3(d, -d, -d), new Vec3(-d, d, -d), new Vec3(-d, -d, d)};
        DustKind[] kinds = DustKind.values();
        DustCloud[] clouds = new DustCloud[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            Vec3 noiseOffset = new Vec3(1_000 + 1_000 * i, 2_000 + 1_000 * i, 3_000 + 1_000 * i);
            clouds[i] = new DustCloud(center.add(offsets[i]), POCKET_RADIUS, kinds[i], POCKET_INTENSITY, noiseOffset);
        }
        return List.of(clouds);
    }

    /**
     * Saved on the deep space level: the rule's value when the world was created.
     *
     * @param enabled whether this world has the nebula
     */
    public record Locked(boolean enabled) {
        public static final Codec<Locked> CODEC = Codec.BOOL.xmap(Locked::new, Locked::enabled);
    }
}
