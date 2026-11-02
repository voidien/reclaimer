package com.voidreclaimer.body;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Big bodies against the world: the move a body's hit boxes allow, for flight. A body with no boxes is left to its vanilla box. The
 * vanilla box still runs afterwards, for everything else that reads it.
 */
public final class BoxCollision {
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
        BodyData data = view.data();
        List<BodyBoxes.Box> body = BodyBoxes.place(view.chassis(), socket -> !data.section(socket).isEmpty(), player.position(), player.getYRot(), player.getXRot(), view.stats().scale());
        List<BodyBoxes.Box> blocks = blocksNear(player.level(), body, velocity);
        Vec3 allowed = BodyBoxes.resolve(body, blocks, velocity);
        lastBlocks = blocks.size();
        lastMicros = (System.nanoTime() - start) / 1000L;
        return allowed;
    }

    /** Every solid block any box could reach this tick. Space is mostly empty, so whole empty sections are skipped first. */
    static List<BodyBoxes.Box> blocksNear(Level level, List<BodyBoxes.Box> body, Vec3 velocity) {
        List<BodyBoxes.Box> blocks = new ArrayList<>();
        LongSet seen = new LongOpenHashSet();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (BodyBoxes.Box box : body) {
            AABB reach = around(box).expandTowards(velocity).inflate(1.0E-3);
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
                                        add(blocks, state.getCollisionShape(level, pos), pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private static void add(List<BodyBoxes.Box> blocks, VoxelShape shape, BlockPos pos) {
        if (!shape.isEmpty()) {
            for (AABB part : shape.toAabbs()) {
                blocks.add(BodyBoxes.Box.of(part.move(pos)));
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
