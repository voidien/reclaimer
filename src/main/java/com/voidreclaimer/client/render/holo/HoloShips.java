package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * Miniatures of ships. Blocks are copied on the render thread now and then, and meshed in the background only when the copy
 * differs, so a ship that stays as it is costs a replay and nothing else. Only blocks with a model are shown: what a block
 * entity draws (Create's shafts and cogs) is not in the miniature.
 */
final class HoloShips {
    /** The most blocks copied from a ship. */
    static final int MAX_BLOCKS = 4096;
    /** Ticks between looks at a ship's blocks. */
    private static final int LOOK_TICKS = 40;
    /** A time that is always due, at any game time; a far earlier one would overflow in {@link #due}. */
    static final long NEVER = -LOOK_TICKS;
    /** The most ships kept meshed. */
    private static final int CACHED = 8;
    private static final double SEARCH_RANGE = 64;

    private static final class Entry {
        @Nullable
        HoloMesh mesh;
        long lookedAt = NEVER;
        int hash;
        boolean baking;
    }

    private static final Map<UUID, Entry> ENTRIES = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Entry> eldest) {
            return size() > CACHED;
        }
    };

    private HoloShips() {}

    /** Whether a ship's blocks should be looked at again. */
    static boolean due(long lookedAt, long gameTime) {
        return gameTime - lookedAt >= LOOK_TICKS;
    }

    /** The ship a projector stands on, or else the nearest within {@link #SEARCH_RANGE} blocks; null with neither. */
    @Nullable
    static SubLevel shipFor(Level level, BlockPos projector) {
        SubLevel own = Ships.shipAt(level, projector);
        if (own != null) {
            return own;
        }
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        Vec3 from = Vec3.atCenterOf(projector);
        SubLevel best = null;
        double bestDistance = SEARCH_RANGE * SEARCH_RANGE;
        for (SubLevel ship : container.getAllSubLevels()) {
            var at = ship.logicalPose().position();
            double distance = from.distanceToSqr(at.x(), at.y(), at.z());
            if (!ship.isRemoved() && distance < bestDistance) {
                best = ship;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The newest mesh of a ship, null until the first is ready. */
    @Nullable
    static HoloMesh mesh(SubLevel ship, long gameTime) {
        Entry entry = ENTRIES.computeIfAbsent(ship.getUniqueId(), id -> new Entry());
        if (!entry.baking && due(entry.lookedAt, gameTime)) {
            entry.lookedAt = gameTime;
            Map<BlockPos, BlockState> blocks = copy(ship);
            int hash = blocks.hashCode();
            if (entry.mesh == null || hash != entry.hash) {
                entry.hash = hash;
                entry.baking = true;
                CompletableFuture.supplyAsync(() -> bake(blocks), Util.backgroundExecutor()).whenComplete((mesh, error) -> {
                    Minecraft.getInstance().execute(() -> {
                        entry.baking = false;
                        if (error != null) {
                            LogUtils.getLogger().warn("A ship's hologram could not be meshed", error);
                        } else {
                            entry.mesh = mesh;
                        }
                    });
                });
            }
        }
        return entry.mesh;
    }

    private static Map<BlockPos, BlockState> copy(SubLevel ship) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (PlotChunkHolder holder : ship.getPlot().getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            int originX = holder.getPos().getMinBlockX();
            int originZ = holder.getPos().getMinBlockZ();
            LevelChunkSection[] sections = chunk.getSections();
            // This runs on the render thread: a ship is mostly empty air, so whole sections are skipped.
            for (int index = 0; index < sections.length; index++) {
                LevelChunkSection section = sections[index];
                if (section.hasOnlyAir()) {
                    continue;
                }
                int originY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
                for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                    for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
                        for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                            BlockState state = section.getBlockState(x, y, z);
                            if (!state.isAir()) {
                                blocks.put(new BlockPos(originX + x, originY + y, originZ + z), state);
                                if (blocks.size() >= MAX_BLOCKS) {
                                    return blocks;
                                }
                            }
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private static HoloMesh bake(Map<BlockPos, BlockState> blocks) {
        HoloMesh mesh = new HoloMesh();
        HoloShipRegion region = new HoloShipRegion(blocks);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        PoseStack pose = new PoseStack();
        // A ship's plot lies millions of blocks out, far past what a float holds exactly.
        BlockPos origin = blocks.keySet().stream().findAny().orElse(BlockPos.ZERO);
        blocks.forEach((pos, state) -> {
            if (state.getRenderShape() == RenderShape.MODEL) {
                pose.pushPose();
                pose.translate(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
                dispatcher.renderBatched(state, pos, region, pose, mesh, true, random);
                pose.popPose();
            }
        });
        return mesh;
    }
}
