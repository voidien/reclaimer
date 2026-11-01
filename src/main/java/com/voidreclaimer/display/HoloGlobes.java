package com.voidreclaimer.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.EarthSky;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.earth.Sectors;
import java.util.List;
import net.minecraft.server.MinecraftServer;

/**
 * The two globes a Holo Projector can show, and the progress they show. A client only has the Dyson counters in deep space
 * and the sky era on Earth, so the server samples both into a {@link Progress} and sends it with the block; the projector's
 * usual "only when it changed" then covers it.
 */
public final class HoloGlobes {
    public static final String DYSON = "dyson";
    public static final String EARTH = "earth";
    public static final List<String> NAMES = List.of(DYSON, EARTH);

    /**
     * What a globe shows.
     *
     * @param satellites swarm satellites launched
     * @param trusses    shell truss modules launched
     * @param tier       the sphere's tier
     * @param shell      how much of the shell stands, 0 to 1
     * @param restored   one bit per restored sector, as in {@link SectorMap}
     * @param era        the {@code SkyEra} of ruined Earth, as its ordinal
     */
    public record Progress(long satellites, long trusses, int tier, float shell, int restored, int era) {
        public static final Progress NONE = new Progress(0, 0, 0, 0f, 0, 0);
        public static final Codec<Progress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.optionalFieldOf("satellites", 0L).forGetter(Progress::satellites),
                Codec.LONG.optionalFieldOf("trusses", 0L).forGetter(Progress::trusses),
                Codec.INT.optionalFieldOf("tier", 0).forGetter(Progress::tier),
                Codec.FLOAT.optionalFieldOf("shell", 0f).forGetter(Progress::shell),
                Codec.INT.optionalFieldOf("restored", 0).forGetter(Progress::restored),
                Codec.INT.optionalFieldOf("era", 0).forGetter(Progress::era)
        ).apply(instance, Progress::new));

        public int restoredCount() {
            return new SectorMap(restored).count();
        }

        public boolean isRestored(int sector) {
            return new SectorMap(restored).isRestored(sector);
        }
    }

    private HoloGlobes() {}

    public static Progress sample(MinecraftServer server) {
        DysonSphereData sphere = DysonSphereData.of(server);
        return new Progress(sphere.satellites(), sphere.trusses(), sphere.tier(), sphere.shellProgress(), EarthRestoration.map(server).restored(),
                EarthSky.get(server).ordinal());
    }

    /** The lines over a globe. */
    public static List<String> caption(String globe, Progress progress) {
        if (EARTH.equals(globe)) {
            return List.of("RUINED EARTH", "Restored " + progress.restoredCount() + " / " + Sectors.COUNT);
        }
        return List.of("DYSON SPHERE  TIER " + progress.tier(), "Satellites " + progress.satellites(),
                "Trusses " + progress.trusses() + "  Shell " + Readouts.percent(progress.shell()));
    }
}
