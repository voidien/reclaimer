package com.voidreclaimer.debug;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperLinks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What is moving around a base, as photo mode's overlay shows it: each helper drone's route with its job and phase, and the Create
 * rotation and stress of the machines nearby. Read on the server. Why something is not working is usually in one of these.
 */
public final class LogisticsOverlay {
    public static final int ROUTE = 0xFF60D0FF;
    public static final int HEADING = 0xFFFFE060;
    public static final int TURNING = 0xFF80E080;
    public static final int STOPPED = 0xFF909090;
    public static final int OVERSTRESSED = 0xFFFF4040;
    /** How many chunks out to look for machines: a whole factory, without walking every block entity in the world. */
    private static final int MACHINE_CHUNKS = 3;

    private LogisticsOverlay() {}

    /** Adds every drone within {@code range} blocks of {@code around}, and the machines in the chunks near it. */
    public static void add(ServerLevel level, Vec3 around, double range, DebugShapes shapes, List<String> facts) {
        for (HelperDrone drone : level.getEntitiesOfClass(HelperDrone.class, new AABB(around, around).inflate(range))) {
            drone(drone, shapes, facts);
        }
        machines(level, around, shapes, facts);
    }

    private static void drone(HelperDrone drone, DebugShapes shapes, List<String> facts) {
        Vec3 at = drone.position();
        HelperLinks links = drone.links();
        List<BlockPos> stops = new ArrayList<>();
        links.source().ifPresent(stops::add);
        links.mine().ifPresent(stops::add);
        links.destination().ifPresent(stops::add);
        links.collect().ifPresent(stops::add);
        for (int i = 0; i + 1 < stops.size(); i++) {
            shapes.line(Vec3.atCenterOf(stops.get(i)), Vec3.atCenterOf(stops.get(i + 1)), ROUTE);
        }
        drone.target().ifPresent(target -> shapes.arrow(at, target.subtract(at), HEADING));
        Vec3 leg = at;
        for (Vec3 waypoint : drone.waypoints()) {
            shapes.line(leg, waypoint, HEADING);
            leg = waypoint;
        }
        String job = links.collecting() ? "collecting, " + drone.collectPhase() : links.mining() ? "mining, " + drone.minePhase()
                : links.complete() ? "ferrying, " + drone.phase() : "idle, no links";
        String cargo = drone.carried().isEmpty() ? "" : ", carrying " + drone.carried().getCount() + " " + drone.carried().getHoverName().getString();
        String line = "Drone " + job + cargo + (drone.blocked() ? ", blocked" : "") + ", battery " + Math.round(drone.battery() * 100f) + "%";
        shapes.text(at.add(0, 0.8, 0), line, HEADING);
        facts.add(line + String.format(Locale.ROOT, " at %.1f %.1f %.1f", at.x, at.y, at.z));
    }

    private static void machines(ServerLevel level, Vec3 around, DebugShapes shapes, List<String> facts) {
        ChunkPos centre = new ChunkPos(BlockPos.containing(around));
        for (int cx = centre.x - MACHINE_CHUNKS; cx <= centre.x + MACHINE_CHUNKS; cx++) {
            for (int cz = centre.z - MACHINE_CHUNKS; cz <= centre.z + MACHINE_CHUNKS; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (entity instanceof KineticBlockEntity kinetic) {
                        machine(kinetic, shapes, facts);
                    }
                }
            }
        }
    }

    private static void machine(KineticBlockEntity kinetic, DebugShapes shapes, List<String> facts) {
        float speed = kinetic.getSpeed();
        boolean over = kinetic.isOverStressed();
        String text = over ? "OVERSTRESSED" : String.format(Locale.ROOT, "%.0f rpm", speed);
        shapes.text(Vec3.atCenterOf(kinetic.getBlockPos()), text, over ? OVERSTRESSED : speed == 0f ? STOPPED : TURNING);
        if (over || speed == 0f) {
            facts.add("Machine at " + kinetic.getBlockPos().toShortString() + ": " + text);
        }
    }
}
