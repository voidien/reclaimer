package com.voidreclaimer.ship;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.ShipCrashEvent;
import com.voidreclaimer.body.CrashRules;
import com.voidreclaimer.registry.ModBlocks;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;

/**
 * Ships breaking things. Sable asks a block's callback about each contact while it steps the physics, and changing a block in there can
 * hang the server, so this only writes the hit down. The blocks go at the end of the tick, the weakest first, by the same rule as a body's
 * crash. Sable's own fragile blocks, which every block's callback would otherwise hide, go on working.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ShipCrash implements BlockSubLevelCollisionCallback {
    public static final ShipCrash INSTANCE = new ShipCrash();
    private static final Queue<Impact> IMPACTS = new ConcurrentLinkedQueue<>();
    /** A ship with no mass on record counts as this. */
    private static final double NO_MASS = 10.0;

    private record Impact(ResourceKey<Level> level, BlockPos hit, BlockPos other, double speed) {}

    /** One block that a hit could break. */
    private record Target(ServerLevel level, BlockPos pos, double speed, double hardest, float hardness) {}

    private ShipCrash() {}

    @Override
    public CollisionResult sable$onCollision(BlockPos hit, BlockPos other, Vector3d at, double speed) {
        CollisionResult fragile = FragileBlockCallback.INSTANCE.sable$onCollision(hit, other, at, speed);
        double along = Math.abs(speed);
        if (along > Config.CRASH_SAFE_SPEED.get() && Config.CRASH_BREAKS_BLOCKS.get()) {
            IMPACTS.add(new Impact(SubLevelPhysicsSystem.getCurrentlySteppingSystem().getLevel().dimension(), hit.immutable(), other.immutable(), along));
        }
        return fragile;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (IMPACTS.isEmpty()) {
            return;
        }
        List<Target> targets = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        double safe = Config.CRASH_SAFE_SPEED.get();
        for (Impact impact = IMPACTS.poll(); impact != null; impact = IMPACTS.poll()) {
            ServerLevel level = event.getServer().getLevel(impact.level());
            if (level != null) {
                double hardest = CrashRules.hardness(impact.speed(), Math.max(mass(level, impact.hit()), mass(level, impact.other())), safe);
                add(targets, seen, level, impact.hit(), impact.speed(), hardest);
                add(targets, seen, level, impact.other(), impact.speed(), hardest);
            }
        }
        targets.stream().sorted(Comparator.comparingDouble(Target::hardness)).limit(Config.CRASH_MOST_BLOCKS.get()).forEach(ShipCrash::breakBlock);
    }

    private static void add(List<Target> targets, Set<BlockPos> seen, ServerLevel level, BlockPos pos, double speed, double hardest) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (!state.isAir() && hardness >= 0f && hardness <= hardest && !state.is(ModBlocks.CRASH_PROOF) && seen.add(pos)) {
            targets.add(new Target(level, pos, speed, hardest, hardness));
        }
    }

    private static void breakBlock(Target target) {
        if (!NeoForge.EVENT_BUS.post(new ShipCrashEvent(target.level(), target.pos(), target.speed())).isCanceled()) {
            target.level().destroyBlock(target.pos(), true);
        }
    }

    private static double mass(ServerLevel level, BlockPos pos) {
        ServerSubLevel ship = Ships.shipAt(level, pos);
        return ship == null || ship.getMassTracker() == null ? NO_MASS : ship.getMassTracker().getMass();
    }

    /** What a tick's impacts are waiting for: for a test that wants to know the queue has been read. */
    public static boolean idle(MinecraftServer server) {
        return IMPACTS.isEmpty();
    }
}
