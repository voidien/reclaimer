package com.voidreclaimer.asteroid;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.joml.Vector3d;

/**
 * Too much drilling in one place splits the rock: a lump of it breaks off as a real body and drifts away, to be chased, or to be dodged. The
 * count of holes is lost on a restart, which only means a rock holds out a little longer.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Fracture {
    /** Holes count together within a cube of this many blocks. */
    private static final int AREA = 32;
    private static final int SMALLEST_PIECE = 8;
    private static final String PIECE_NAME = "void_reclaimer:rock_fragment";
    private static final double SLOWEST = 1.0;
    private static final double FASTEST = 3.0;
    private static final double MOST_SPIN = 0.4;
    private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> HOLES = new HashMap<>();

    private Fracture() {}

    /** A block of rock was drilled out at {@code pos}. */
    public static void drilled(ServerLevel level, BlockPos pos) {
        if (!Config.ASTEROID_FRACTURES.get() || Ships.inPlots(level, pos)) {
            return;
        }
        Long2IntOpenHashMap holes = HOLES.computeIfAbsent(level.dimension(), key -> new Long2IntOpenHashMap());
        long area = BlockPos.asLong(Math.floorDiv(pos.getX(), AREA), Math.floorDiv(pos.getY(), AREA), Math.floorDiv(pos.getZ(), AREA));
        if (holes.addTo(area, 1) + 1 >= Config.ASTEROID_FRACTURE_HOLES.get()) {
            holes.remove(area);
            breakOff(level, pos);
        }
    }

    /** Up to {@code most} blocks joined to {@code start} that pass {@code rock}, nearest first; {@code start} itself if it is rock. */
    public static List<BlockPos> gather(BlockPos start, Predicate<BlockPos> rock, int most) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> next = new ArrayDeque<>();
        next.add(start);
        seen.add(start);
        while (!next.isEmpty() && found.size() < most) {
            BlockPos pos = next.poll();
            if (rock.test(pos)) {
                found.add(pos);
                for (Direction direction : Direction.values()) {
                    BlockPos beside = pos.relative(direction);
                    if (seen.add(beside)) {
                        next.add(beside);
                    }
                }
            }
        }
        return found;
    }

    private static void breakOff(ServerLevel level, BlockPos hole) {
        if (pieces(level) >= Config.ASTEROID_MOST_PIECES.get()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            List<BlockPos> piece = gather(hole.relative(direction), pos -> level.getBlockState(pos).is(ModBlocks.ASTEROID_BLOCKS), Config.ASTEROID_PIECE_BLOCKS.get());
            if (piece.size() >= SMALLEST_PIECE) {
                send(level, hole, piece);
                return;
            }
        }
    }

    private static int pieces(ServerLevel level) {
        int pieces = 0;
        for (ServerSubLevel ship : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            pieces += PIECE_NAME.equals(ship.getName()) ? 1 : 0;
        }
        return pieces;
    }

    private static void send(ServerLevel level, BlockPos hole, List<BlockPos> piece) {
        BlockPos min = piece.get(0);
        BlockPos max = piece.get(0);
        Vec3 middle = Vec3.ZERO;
        for (BlockPos pos : piece) {
            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
            middle = middle.add(Vec3.atCenterOf(pos));
        }
        middle = middle.scale(1.0 / piece.size());
        ServerSubLevel ship = SubLevelAssemblyHelper.assembleBlocks(level, piece.get(0), piece, new BoundingBox3i(min, max));
        if (ship == null) {
            return;
        }
        ship.setName(PIECE_NAME);
        RandomSource random = level.getRandom();
        Vec3 away = middle.subtract(Vec3.atCenterOf(hole));
        Vec3 way = away.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 1.0, 0.0) : away.normalize();
        Vector3d speed = new Vector3d(way.x, way.y, way.z).mul(SLOWEST + random.nextDouble() * (FASTEST - SLOWEST));
        Vector3d spin = new Vector3d(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).mul(MOST_SPIN);
        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle != null && handle.isValid()) {
            handle.addLinearAndAngularVelocity(speed, spin);
        }
        level.playSound(null, hole, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        HOLES.clear();
    }
}
