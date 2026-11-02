package com.voidreclaimer.ship;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.ShipCrashEvent;
import com.voidreclaimer.body.CrashRules;
import com.voidreclaimer.registry.ModBlocks;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;

/**
 * Ships breaking things. Each tick the server compares a ship's speed with the last tick's: a ship that lost a lot of speed all at once has
 * hit something. Only then does it look at what: the hull's blocks that ran into the world's, the weakest first, by the same rule as a
 * body's crash. Nothing here touches Sable's collision callbacks: one on every block turns off its collider culling, and changing a block
 * inside the physics step can hang the server.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ShipCrash {
    /** How far past its own blocks a ship reaches into what it hit, in blocks. */
    private static final double REACH = 1.5;
    /** A ship with no mass on record counts as this. */
    private static final double NO_MASS = 10.0;
    private static final Map<UUID, Vector3d> LAST_SPEED = new HashMap<>();

    /** One block a hit could break. */
    private record Target(BlockPos pos, float hardness) {}

    private ShipCrash() {}

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (!Config.CRASH_BREAKS_BLOCKS.get()) {
            return;
        }
        Set<UUID> here = new HashSet<>();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerSubLevel ship : SubLevelContainer.getContainer(level).getAllSubLevels()) {
                here.add(ship.getUniqueId());
                look(level, ship);
            }
        }
        LAST_SPEED.keySet().retainAll(here);
    }

    private static void look(ServerLevel level, ServerSubLevel ship) {
        RigidBodyHandle body = RigidBodyHandle.of(ship);
        if (body == null || !body.isValid()) {
            return;
        }
        Vector3d now = body.getLinearVelocity(new Vector3d());
        Vector3d before = LAST_SPEED.put(ship.getUniqueId(), now);
        if (before != null) {
            double lost = CrashRules.speedLost(before.x, before.y, before.z, now.x, now.y, now.z);
            if (lost > Config.CRASH_SAFE_SPEED.get()) {
                hit(level, ship, new Vec3(before.x, before.y, before.z).normalize(), lost);
            }
        }
    }

    private static void hit(ServerLevel level, ServerSubLevel ship, Vec3 way, double speed) {
        double mass = ship.getMassTracker() == null ? NO_MASS : ship.getMassTracker().getMass();
        double hardest = CrashRules.hardness(speed, mass, Config.CRASH_SAFE_SPEED.get());
        Set<BlockPos> seen = new HashSet<>();
        List<Target> targets = new ArrayList<>();
        for (BlockPos block : com.voidreclaimer.station.StationAssembly.blocksOf(level, ship)) {
            Vec3 at = ship.logicalPose().transformPosition(Vec3.atCenterOf(block));
            for (double ahead = 0.5; ahead <= REACH; ahead += 0.5) {
                BlockPos met = BlockPos.containing(at.add(way.scale(ahead)));
                if (!level.getBlockState(met).isAir() && !Ships.inPlots(level, met)) {
                    add(targets, seen, level, met, hardest);
                    add(targets, seen, level, block.immutable(), hardest);
                    break;
                }
            }
        }
        targets.stream().sorted(Comparator.comparingDouble(Target::hardness)).limit(CrashRules.blocks(speed, mass, Config.CRASH_SAFE_SPEED.get(), Config.CRASH_MOST_BLOCKS.get()))
                .forEach(target -> breakBlock(level, target.pos(), speed));
    }

    private static void add(List<Target> targets, Set<BlockPos> seen, ServerLevel level, BlockPos pos, double hardest) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (!state.isAir() && hardness >= 0f && hardness <= hardest && !state.is(ModBlocks.CRASH_PROOF) && seen.add(pos)) {
            targets.add(new Target(pos, hardness));
        }
    }

    private static void breakBlock(ServerLevel level, BlockPos pos, double speed) {
        if (!NeoForge.EVENT_BUS.post(new ShipCrashEvent(level, pos, speed)).isCanceled()) {
            level.destroyBlock(pos, true);
        }
    }
}
