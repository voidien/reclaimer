package com.voidreclaimer.ship;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

/**
 * Finding a block's ship and its thrusters. A Sable ship (a sub-level) lives in its own plot of chunks far away in the
 * same dimension; a block inside the plot belongs to that ship, in the ship's own coordinates.
 */
public final class Ships {
    /** The biggest ship we search for thrusters, in chunks from the plot's centre; larger ships keep their far parts dark. */
    static final int SEARCH_CHUNKS = 8;

    private Ships() {}

    /** The ship the block at {@code pos} is part of, or null when the block sits in the normal world. */
    @Nullable
    public static ServerSubLevel shipAt(ServerLevel level, BlockPos pos) {
        SubLevelContainer container = SubLevelContainer.getContainer((Level) level);
        if (container == null || !container.inBounds(pos)) {
            return null;
        }
        LevelPlot plot = container.getPlot(new ChunkPos(pos));
        SubLevel ship = plot == null ? null : plot.getSubLevel();
        return ship instanceof ServerSubLevel server && !server.isRemoved() ? server : null;
    }

    /** Whether {@code pos} lies in Sable's plot region at all (a ship's own coordinates), whether or not a ship is there. */
    public static boolean inPlots(Level level, BlockPos pos) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        return container != null && container.inBounds(pos);
    }

    /** The ship the block at {@code pos} is part of on either side, or null in the normal world. */
    @Nullable
    public static SubLevel shipAt(Level level, BlockPos pos) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || !container.inBounds(pos)) {
            return null;
        }
        LevelPlot plot = container.getPlot(new ChunkPos(pos));
        SubLevel ship = plot == null ? null : plot.getSubLevel();
        // A removed ship's plot lingers for a while: its blocks are nowhere in the world any more.
        return ship instanceof ServerSubLevel server && server.isRemoved() ? null : ship;
    }

    /**
     * Whether a ship's solid hull is straight above {@code from}. Sky light and heightmaps cannot tell, since a ship's blocks are in
     * its plot, so only a ray finds them. A pane of glass lets the sun through, as it does in the world.
     */
    public static boolean hullOverhead(Level level, Vec3 from, Entity looker) {
        Vec3 top = new Vec3(from.x, level.getMaxBuildHeight(), from.z);
        BlockHitResult hit = level.clip(new ClipContext(from, top, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, looker));
        return hit.getType() == HitResult.Type.BLOCK && shipAt(level, hit.getBlockPos()) != null
                && level.getBlockState(hit.getBlockPos()).canOcclude();
    }

    /** Where the ship's block entities are, in its own coordinates: not its plain blocks, see {@code StationAssembly.blocksOf}. */
    public static Iterable<BlockPos> blocksOf(ServerLevel level, ServerSubLevel ship) {
        LevelPlot plot = ship.getPlot();
        ChunkPos min = plot.getChunkMin();
        ChunkPos max = plot.getChunkMax();
        ChunkPos centre = new ChunkPos(plot.getCenterBlock());
        int x0 = Math.max(min.x, centre.x - SEARCH_CHUNKS), x1 = Math.min(max.x, centre.x + SEARCH_CHUNKS);
        int z0 = Math.max(min.z, centre.z - SEARCH_CHUNKS), z1 = Math.min(max.z, centre.z + SEARCH_CHUNKS);
        List<BlockPos> out = new ArrayList<>();
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    out.add(entity.getBlockPos());
                }
            }
        }
        return out;
    }

    /** Every thruster block entity of {@code ship}. */
    public static List<ThrusterLike> thrustersOf(ServerLevel level, ServerSubLevel ship) {
        List<ThrusterLike> out = new ArrayList<>();
        for (BlockPos pos : blocksOf(level, ship)) {
            if (level.getBlockEntity(pos) instanceof ThrusterLike thruster) {
                out.add(thruster);
            }
        }
        return out;
    }

    /** A unit vector along a block direction, as a JOML vector. */
    public static Vector3d unit(Direction direction) {
        return new Vector3d(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    /** The ship's rotation, ship frame to world. */
    public static Quaterniondc orientation(ServerSubLevel ship) {
        return ship.logicalPose().orientation();
    }
}
