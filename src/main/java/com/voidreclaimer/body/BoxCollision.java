package com.voidreclaimer.body;

import com.voidreclaimer.api.event.BodyCollisionEvent;
import com.voidreclaimer.link.BodyHost;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3d;

/**
 * Big bodies against blocks and ships: the move a body's hit boxes allow, for flight. A body with no boxes is left to its vanilla box. The
 * vanilla box still runs afterwards, for everything else that reads it.
 */
public final class BoxCollision {
    private static final double MARGIN = 1.0E-3;
    /** How long the last check took, for the overlay. */
    private static volatile long lastMicros;
    private static volatile int lastBlocks;

    private BoxCollision() {}

    public static long lastMicros() {
        return lastMicros;
    }

    public static int lastBlocks() {
        return lastBlocks;
    }

    /** {@code velocity} if nothing is in the way, the same object, or as much of it as the body's boxes allow. */
    public static Vec3 resolve(Player player, Vec3 velocity) {
        BodyView view = Bodies.view(player);
        if (view.chassis().sockets().stream().allMatch(socket -> socket.boxes().isEmpty()) || velocity.lengthSqr() == 0.0) {
            return velocity;
        }
        long start = System.nanoTime();
        List<BodyBoxes.Box> body = BodyTurning.place(player, view, BodyTurning.of(player));
        List<BodyBoxes.Box> blocks = solidsNear(player, body, velocity, MARGIN);
        Vec3 allowed = BodyBoxes.resolve(body, blocks, velocity);
        if (allowed != velocity) {
            NeoForge.EVENT_BUS.post(new BodyCollisionEvent(player, velocity, allowed));
        }
        lastBlocks = blocks.size();
        lastMicros = (System.nanoTime() - start) / 1000L;
        return allowed;
    }

    /** All a body can hit this tick: blocks, ships and other bodies. */
    static List<BodyBoxes.Box> solidsNear(Player player, List<BodyBoxes.Box> body, Vec3 velocity, double margin) {
        List<BodyBoxes.Box> solids = blocksNear(player.level(), body, velocity, margin);
        bodies(player, body, velocity, margin, solids);
        return solids;
    }

    /** Other bodies near this one: a big one by its own boxes, and anything smaller by its vanilla box. */
    private static void bodies(Player player, List<BodyBoxes.Box> body, Vec3 velocity, double margin, List<BodyBoxes.Box> solids) {
        AABB all = null;
        for (BodyBoxes.Box box : body) {
            AABB reach = around(box).expandTowards(velocity).inflate(margin);
            all = all == null ? reach : all.minmax(reach);
        }
        if (all == null) {
            return;
        }
        for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, all,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator() && (entity instanceof Player || entity instanceof BodyHost))) {
            BodyView view = Bodies.view(other);
            BodyData data = view.data();
            List<BodyBoxes.Box> boxes = BodyBoxes.place(view.chassis(), socket -> !data.section(socket).isEmpty(), other.position(), other.getYRot(), other.getXRot(), view.stats().scale());
            if (boxes.isEmpty()) {
                solids.add(BodyBoxes.Box.of(other.getBoundingBox()));
            } else {
                solids.addAll(boxes);
            }
        }
    }

    /** Every solid block any box could reach this tick, in the world and on any ship near it. */
    static List<BodyBoxes.Box> blocksNear(Level level, List<BodyBoxes.Box> body, Vec3 velocity, double margin) {
        List<BodyBoxes.Box> blocks = new ArrayList<>();
        LongSet seen = new LongOpenHashSet();
        AABB all = null;
        for (BodyBoxes.Box box : body) {
            AABB reach = around(box).expandTowards(velocity).inflate(margin);
            all = all == null ? reach : all.minmax(reach);
            scan(level, reach, seen, (pos, part) -> blocks.add(BodyBoxes.Box.of(part.move(pos))));
        }
        if (all != null) {
            ships(level, all, body, velocity, margin, blocks);
        }
        return blocks;
    }

    /**
     * A ship's blocks are in its plot, far from where they are drawn, so each body box is carried into the ship's own coordinates to
     * find them, and each block comes back as a box turned with the ship.
     */
    private static void ships(Level level, AABB all, List<BodyBoxes.Box> body, Vec3 velocity, double margin, List<BodyBoxes.Box> blocks) {
        for (SubLevelAccess ship : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(all))) {
            Pose3dc pose = ship.logicalPose();
            LongSet seen = new LongOpenHashSet();
            for (BodyBoxes.Box box : body) {
                scan(level, inPlot(pose, box, velocity, margin), seen, (pos, part) -> blocks.add(onShip(pose, part.move(pos))));
            }
        }
    }

    /** What {@code box} could reach this tick, in the ship's own coordinates. */
    private static AABB inPlot(Pose3dc pose, BodyBoxes.Box box, Vec3 velocity, double margin) {
        AABB plot = null;
        for (Vec3 corner : BodyBoxes.corners(box)) {
            for (Vec3 at : List.of(corner, corner.add(velocity))) {
                Vec3 local = pose.transformPositionInverse(at);
                AABB point = new AABB(local, local);
                plot = plot == null ? point : plot.minmax(point);
            }
        }
        return plot.inflate(margin);
    }

    /** A block's shape on a ship, as a box in the world that turns with the ship. */
    static BodyBoxes.Box onShip(Pose3dc pose, AABB part) {
        Vec3 middle = pose.transformPosition(part.getCenter());
        return new BodyBoxes.Box("", new Vector3d(middle.x, middle.y, middle.z), new Vector3d(part.getXsize() / 2.0, part.getYsize() / 2.0, part.getZsize() / 2.0),
                pose.orientation());
    }

    /** Calls {@code found} with each solid part in {@code reach}. Space is mostly empty, so whole empty sections are skipped first. */
    private static void scan(Level level, AABB reach, LongSet seen, java.util.function.BiConsumer<BlockPos, AABB> found) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int fromX = BlockPos.containing(reach.minX, 0, 0).getX();
        int toX = BlockPos.containing(reach.maxX, 0, 0).getX();
        int fromZ = BlockPos.containing(0, 0, reach.minZ).getZ();
        int toZ = BlockPos.containing(0, 0, reach.maxZ).getZ();
        int fromY = Math.max(level.getMinBuildHeight(), BlockPos.containing(0, reach.minY, 0).getY());
        int toY = Math.min(level.getMaxBuildHeight() - 1, BlockPos.containing(0, reach.maxY, 0).getY());
        for (int cx = SectionPos.blockToSectionCoord(fromX); cx <= SectionPos.blockToSectionCoord(toX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(fromZ); cz <= SectionPos.blockToSectionCoord(toZ); cz++) {
                if (!(level.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) {
                    continue;
                }
                for (int sy = SectionPos.blockToSectionCoord(fromY); sy <= SectionPos.blockToSectionCoord(toY); sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir()) {
                        continue;
                    }
                    int minX = Math.max(fromX, SectionPos.sectionToBlockCoord(cx));
                    int maxX = Math.min(toX, SectionPos.sectionToBlockCoord(cx) + 15);
                    int minZ = Math.max(fromZ, SectionPos.sectionToBlockCoord(cz));
                    int maxZ = Math.min(toZ, SectionPos.sectionToBlockCoord(cz) + 15);
                    int minY = Math.max(fromY, SectionPos.sectionToBlockCoord(sy));
                    int maxY = Math.min(toY, SectionPos.sectionToBlockCoord(sy) + 15);
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (!state.isAir() && seen.add(BlockPos.asLong(x, y, z))) {
                                    pos.set(x, y, z);
                                    VoxelShape shape = state.getCollisionShape(level, pos);
                                    if (!shape.isEmpty()) {
                                        for (AABB part : shape.toAabbs()) {
                                            found.accept(pos.immutable(), part);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static AABB around(BodyBoxes.Box box) {
        List<Vec3> corners = BodyBoxes.corners(box);
        AABB around = new AABB(corners.get(0), corners.get(7));
        for (Vec3 corner : corners) {
            around = around.minmax(new AABB(corner, corner));
        }
        return around;
    }
}
