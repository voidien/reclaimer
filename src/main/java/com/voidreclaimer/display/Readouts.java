package com.voidreclaimer.display;

import com.voidreclaimer.api.holo.HoloReadout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The formatters of the built-in readouts: snapshots to lines, nothing else, so every number and every cap is tested
 * without a world. The samplers that fill the snapshots are in {@link ReadoutKinds}.
 */
public final class Readouts {
    /** Ticks in a Minecraft day. */
    private static final long DAY = 24_000;

    private Readouts() {}

    /** What a kind shows now; an unknown kind shows nothing. */
    public static List<String> lines(@Nullable HoloReadout<?> kind, Level level, BlockPos pos) {
        return kind == null ? List.of() : kind.lines(level, pos);
    }

    /** The source after {@code current} in id order, wrapping round; an unknown current starts from the first. */
    public static ResourceLocation next(List<ResourceLocation> ids, ResourceLocation current) {
        List<ResourceLocation> sorted = ids.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        int index = sorted.indexOf(current);
        return sorted.get((index + 1) % sorted.size());
    }

    /** A number with one decimal, the same in every locale. */
    public static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A share from 0 to 1 as a whole percentage. */
    public static String percent(double share) {
        return Math.round(Math.max(0, Math.min(1, share)) * 100) + "%";
    }

    /** Game time as the clock readout shows it. */
    public record Clock(long gameTime) {}

    public static List<String> clock(Clock clock) {
        long day = clock.gameTime / DAY;
        long ofDay = clock.gameTime % DAY;
        long hours = ofDay / 1000;
        long minutes = ofDay % 1000 * 60 / 1000;
        return List.of("MISSION CLOCK", String.format(Locale.ROOT, "Day %d  %02d:%02d", day, hours, minutes));
    }

    /** What the nearest Helm reads. */
    public record Helm(boolean onShip, int thrusters, double speed, double heading, double pitch, double roll,
                       double fuel, double fuelCapacity, int energy, int energyCapacity) {}

    public static List<String> helm(Helm helm) {
        if (!helm.onShip) {
            return List.of("HELM", "Not aboard a ship");
        }
        return List.of("HELM",
                "Speed " + number(helm.speed) + " b/s",
                "Heading " + Math.round(helm.heading) + "  Pitch " + Math.round(helm.pitch) + "  Roll " + Math.round(helm.roll),
                "Thrusters " + helm.thrusters,
                "Fuel " + Math.round(helm.fuel) + " / " + Math.round(helm.fuelCapacity) + " mB",
                "Energy " + helm.energy + " / " + helm.energyCapacity + " FE");
    }

    /** What the nearest thruster reads; {@code supply} is its fuel or energy, ready to show. */
    public record Thruster(double throttle, double force, String supply) {}

    public static List<String> thruster(Thruster thruster) {
        return List.of("THRUSTER",
                "Throttle " + percent(thruster.throttle),
                "Force " + number(thruster.force),
                thruster.supply);
    }

    /** What the nearest Cloud Siphon reads. */
    public record Siphon(double density, double power, double speed, int items) {}

    public static List<String> siphon(Siphon siphon) {
        return List.of("CLOUD SIPHON",
                "Density " + percent(siphon.density),
                "Power " + percent(siphon.power),
                "Speed " + number(Math.abs(siphon.speed)) + " RPM",
                "Holding " + siphon.items + " items");
    }

    /** What the nearest Electrolyzer reads, tanks in mB. */
    public record Electrolyzer(double progress, double speed, int water, int oxygen, int hydrogen, int capacity) {}

    public static List<String> electrolyzer(Electrolyzer electrolyzer) {
        return List.of("ELECTROLYZER",
                "Cycle " + percent(electrolyzer.progress),
                "Speed " + number(Math.abs(electrolyzer.speed)) + " RPM",
                "Water " + electrolyzer.water + " / " + electrolyzer.capacity + " mB",
                "Oxygen " + electrolyzer.oxygen + " / " + electrolyzer.capacity + " mB",
                "Hydrogen " + electrolyzer.hydrogen + " / " + electrolyzer.capacity + " mB");
    }

    /** What the nearest Spinner reads. */
    public record Spinner(double progress, double speed, int input, int output) {}

    public static List<String> spinner(Spinner spinner) {
        return List.of("SPINNER",
                "Cycle " + percent(spinner.progress),
                "Speed " + number(Math.abs(spinner.speed)) + " RPM",
                "In " + spinner.input + "  Out " + spinner.output);
    }

    /** What the nearest Launch Controller reads. */
    public record Launch(boolean assembled, double progress, int launches, double speed, boolean payload) {}

    public static List<String> launch(Launch launch) {
        if (!launch.assembled) {
            return List.of("LAUNCH COMPLEX", "Not assembled");
        }
        return List.of("LAUNCH COMPLEX",
                launch.payload ? "Wind-up " + percent(launch.progress) : "No payload",
                "Speed " + number(Math.abs(launch.speed)) + " RPM",
                "Launches " + launch.launches);
    }

    /** The sphere's counters; needs no block near the projector. */
    public record Dyson(long satellites, long trusses, long shellTotal, int tier, double shellProgress) {}

    public static List<String> dyson(Dyson dyson) {
        return List.of("DYSON SPHERE",
                "Tier " + dyson.tier,
                "Satellites " + dyson.satellites,
                "Trusses " + dyson.trusses + " / " + dyson.shellTotal,
                "Shell " + percent(dyson.shellProgress));
    }

    /** What the nearest Terraforming Spire reads. */
    public record Spire(boolean running, double progress, int pulses, int radius) {}

    public static List<String> spire(Spire spire) {
        return List.of("TERRAFORMING SPIRE",
                spire.running ? "Pulse " + percent(spire.progress) : "Idle",
                "Pulses " + spire.pulses,
                "Radius " + spire.radius);
    }

    /** What the nearest Backup Core reads. */
    public record Core(String name, int charges, int maxCharges, double sun) {}

    public static List<String> core(Core core) {
        return List.of("BACKUP CORE",
                core.name,
                "Charges " + core.charges + " / " + core.maxCharges,
                "Sunlight " + percent(core.sun));
    }

    /** What the nearest body reads. */
    public record Body(String name, double health, double maxHealth, double energy, double energyCapacity, boolean lowPower) {}

    public static List<String> body(Body body) {
        return List.of(body.name,
                "Health " + number(body.health) + " / " + number(body.maxHealth),
                "Energy " + number(body.energy) + " / " + number(body.energyCapacity),
                body.lowPower ? "LOW POWER" : "Nominal");
    }

    /** The dust cloud at the projector; {@code kind} is empty outside any cloud. */
    public record Cloud(String kind, double density) {}

    public static List<String> cloud(Cloud cloud) {
        if (cloud.kind.isEmpty()) {
            return List.of("DUST CLOUD", "Clear space");
        }
        return List.of("DUST CLOUD", "Kind " + cloud.kind, "Density " + percent(cloud.density));
    }

    /** A block with stored energy or a tank next to the projector: any mod's. A negative capacity means it has none. */
    public record Device(String name, int energy, int energyCapacity, String fluid, int amount, int fluidCapacity) {}

    public static List<String> device(Device device) {
        List<String> lines = new ArrayList<>();
        lines.add(device.name);
        if (device.energyCapacity >= 0) {
            lines.add("Energy " + device.energy + " / " + device.energyCapacity + " FE");
        }
        if (device.fluidCapacity >= 0) {
            lines.add(device.fluid + " " + device.amount + " / " + device.fluidCapacity + " mB");
        }
        return lines;
    }
}
