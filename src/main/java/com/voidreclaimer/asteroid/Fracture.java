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
import java.util.LinkedHashSet;
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
import com.voidreclaimer.api.event.RockFractureEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
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
    /** A piece is this many blocks thick. */
    private static final int DEPTH = 3;
    /** How far ahead must be empty for a piece to leave that way, in blocks. */
    private static final int CLEAR = 24;
    /** How far in or out the next block of a surface can be. */
    private static final int STEP = 2;
    private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> HOLES = new HashMap<>();

    private Fracture() {}

    /** A block of rock was drilled out at {@code pos}. */
    public static void drilled(ServerLevel level, BlockPos pos) {
        if (!Config.ASTEROID_FRACTURES.get() || Ships.inPlots(level, pos)) {
            return;
        }
        Long2IntOpenHashMap holes = HOLES.computeIfAbsent(level.dimension(), key -> new Long2IntOpenHashMap());
        long area = BlockPos.asLong(Math.floorDiv(pos.getX(), AREA), Math.floorDiv(pos.getY(), AREA), Math.floorDiv(pos.getZ(), AREA));
        if (holes.addTo(area, 1) + 1 >= Config.ASTEROID_FRACTURE_HOLES.get() && breakOff(level, pos)) {
            holes.remove(area);
        }
    }

    /**
     * A slab of rock that can leave along {@code way}: blocks with nothing solid ahead of them or beside their way out, joined across the
     * surface from {@code start}, each with the rock behind it to {@code depth}. Cutting it that way, and not as a ball, is what lets it come
     * away: a ball grown from the bottom of a pit is wider than the pit, and stays where it is.
     */
    public static List<BlockPos> carve(BlockPos start, Direction way, Predicate<BlockPos> solid, int most, int depth, int clear) {
        Set<BlockPos> piece = new LinkedHashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        Queue<BlockPos> next = new ArrayDeque<>();
        next.add(start);
        seen.add(start);
        while (!next.isEmpty() && piece.size() < most) {
            BlockPos face = next.poll();
            if (!solid.test(face) || !clearAhead(face, way, solid, clear)) {
                continue;
            }
            // A block with rock right beside its way out stays: a slab that fits a pit exactly jams, as a real one would.
            BlockPos block = face;
            for (int down = 0; roomAhead(face, way, solid, clear) && down < depth && piece.size() < most && solid.test(block); down++) {
                piece.add(block);
                block = block.relative(way.getOpposite());
            }
            for (Direction side : Direction.values()) {
                if (side.getAxis() != way.getAxis()) {
                    // The surface isn't flat: the next face may be a step in or out.
                    for (int step = -STEP; step <= STEP; step++) {
                        BlockPos beside = face.relative(side).relative(way, step);
                        if (seen.add(beside)) {
                            next.add(beside);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(piece);
    }

    private static boolean roomAhead(BlockPos block, Direction way, Predicate<BlockPos> solid, int clear) {
        for (int ahead = 1; ahead <= clear; ahead++) {
            BlockPos there = block.relative(way, ahead);
            for (Direction side : Direction.values()) {
                if (side.getAxis() != way.getAxis() && solid.test(there.relative(side))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean clearAhead(BlockPos block, Direction way, Predicate<BlockPos> solid, int clear) {
        for (int ahead = 1; ahead <= clear; ahead++) {
            if (solid.test(block.relative(way, ahead))) {
                return false;
            }
        }
        return true;
    }

    /** The first way out of the hole with rock behind it. With none, or if a listener says no, the holes stand and the next one tries again. */
    private static boolean breakOff(ServerLevel level, BlockPos hole) {
        if (pieces(level) >= Config.ASTEROID_MOST_PIECES.get()) {
            return false;
        }
        Predicate<BlockPos> solid = pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        Predicate<BlockPos> rock = pos -> level.getBlockState(pos).is(ModBlocks.ASTEROID_BLOCKS);
        for (Direction way : Direction.values()) {
            BlockPos behind = hole.relative(way.getOpposite());
            if (clearAhead(behind, way, solid, CLEAR) && rock.test(behind)) {
                List<BlockPos> piece = carve(behind, way, rock, Config.ASTEROID_PIECE_BLOCKS.get(), DEPTH, CLEAR);
                if (piece.size() >= SMALLEST_PIECE) {
                    if (NeoForge.EVENT_BUS.post(new RockFractureEvent(level, hole, piece)).isCanceled()) {
                        return false;
                    }
                    send(level, hole, piece, way);
                    return true;
                }
            }
        }
        return false;
    }

    private static int pieces(ServerLevel level) {
        int pieces = 0;
        for (ServerSubLevel ship : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            pieces += PIECE_NAME.equals(ship.getName()) ? 1 : 0;
        }
        return pieces;
    }

    private static void send(ServerLevel level, BlockPos hole, List<BlockPos> piece, Direction way) {
        BlockPos min = piece.get(0);
        BlockPos max = piece.get(0);
        for (BlockPos pos : piece) {
            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
        }
        ServerSubLevel ship = SubLevelAssemblyHelper.assembleBlocks(level, piece.get(0), piece, new BoundingBox3i(min, max));
        if (ship == null) {
            return;
        }
        ship.setName(PIECE_NAME);
        RandomSource random = level.getRandom();
        Vector3d speed = new Vector3d(way.getStepX(), way.getStepY(), way.getStepZ()).mul(SLOWEST + random.nextDouble() * (FASTEST - SLOWEST));
        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle != null && handle.isValid()) {
            // No spin: in a pit a slab has no room to turn, and it would jam.
            handle.addLinearAndAngularVelocity(speed, new Vector3d());
        }
        level.playSound(null, hole, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        HOLES.clear();
    }
}
