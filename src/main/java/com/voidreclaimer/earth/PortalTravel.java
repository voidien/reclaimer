package com.voidreclaimer.earth;

import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.SpawnHandler;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Where an Atmospheric Portal sends you. Any portal outside ruined Earth leads to the return gate on the planet (built on
 * first use, at least Ship-sized); the gate leads back out of the portal the trip began at, or to the deep space spawn
 * when there was none. Both ends put you on the far side of the portal from where you came, a few blocks off its plane.
 */
public final class PortalTravel {
    /** The return gate is at least this big, so the Ship can always come home. */
    public static final int MIN_GATE_WIDTH = 12;
    public static final int MIN_GATE_HEIGHT = 6;
    /** Where the return gate stands on the landing area: centered on x = 0, this far along z, one block above the ground. */
    public static final int GATE_Z = -10;

    private PortalTravel() {}

    /** Null when the destination dimension is missing. */
    @Nullable
    public static DimensionTransition destination(ServerLevel from, Entity entity, BlockPos pane) {
        MinecraftServer server = from.getServer();
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        if (earth == null) {
            return null;
        }
        return RuinedEarth.is(from) ? backOut(server, earth, entity) : toEarth(from, earth, entity, pane);
    }

    private static DimensionTransition toEarth(ServerLevel from, ServerLevel earth, Entity entity, BlockPos pane) {
        Optional<PortalShape.Rect> origin = AtmosphericPortals.rectAround(from, pane);
        if (origin.isPresent()) {
            PortalShape.Rect rect = origin.get();
            int side = rect.travelSign(entity.position().subtract(entity.xOld, entity.yOld, entity.zOld));
            PortalLinks links = earth.getData(ModAttachments.PORTAL_LINKS);
            earth.setData(ModAttachments.PORTAL_LINKS, links.withExit(
                    new PortalLinks.Exit(from.dimension(), rect.centerBottom(), rect.normal(side))));
        }
        int width = Math.min(PortalShape.MAX_SIZE, Math.max(MIN_GATE_WIDTH, origin.map(PortalShape.Rect::width).orElse(0)));
        int height = Math.min(PortalShape.MAX_SIZE, Math.max(MIN_GATE_HEIGHT, origin.map(PortalShape.Rect::height).orElse(0)));
        PortalShape.Rect gate = ensureGate(earth, width, height);
        Vec3 position = standingBefore(gate.centerBottom(), Direction.SOUTH, entity);
        return new DimensionTransition(earth, position, Vec3.ZERO, Direction.SOUTH.toYRot(), 0f, DimensionTransition.PLAY_PORTAL_SOUND);
    }

    private static DimensionTransition backOut(MinecraftServer server, ServerLevel earth, Entity entity) {
        Optional<PortalLinks.Exit> exit = earth.getData(ModAttachments.PORTAL_LINKS).exit();
        ServerLevel level = exit.map(PortalLinks.Exit::dimension).map(server::getLevel).orElse(null);
        if (level == null) {
            ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
            if (space == null) {
                return null;
            }
            return new DimensionTransition(space, SpawnHandler.spawnPosition(), Vec3.ZERO, entity.getYRot(), 0f, DimensionTransition.PLAY_PORTAL_SOUND);
        }
        PortalLinks.Exit where = exit.get();
        return new DimensionTransition(level, standingBefore(where.base(), where.side(), entity), Vec3.ZERO,
                where.side().toYRot(), 0f, DimensionTransition.PLAY_PORTAL_SOUND);
    }

    /** A spot {@code side} of a portal's floor point, far enough off the plane for the entity's width. */
    static Vec3 standingBefore(Vec3 base, Direction side, Entity entity) {
        double distance = 2.0 + entity.getBbWidth();
        return base.add(side.getStepX() * distance, 0, side.getStepZ() * distance);
    }

    /** The return gate, built (frame, panes and a level floor under it) if it is missing or smaller than asked for. */
    public static PortalShape.Rect ensureGate(ServerLevel earth, int width, int height) {
        PortalLinks links = earth.getData(ModAttachments.PORTAL_LINKS);
        if (links.gate().isPresent()) {
            PortalLinks.Gate existing = links.gate().get();
            BlockPos probe = existing.bottomLeft().above(existing.height() / 2).east(existing.width() / 2);
            if (existing.width() >= width && existing.height() >= height && earth.getBlockState(probe).is(ModBlocks.ATMOSPHERIC_PORTAL.get())) {
                return existing.rect();
            }
        }
        BlockPos bottomLeft = new BlockPos(-width / 2, RuinedTerrain.BASE_Y + 1, GATE_Z);
        PortalShape.Rect rect = new PortalShape.Rect(bottomLeft, Direction.Axis.X, width, height);
        build(earth, rect);
        earth.setData(ModAttachments.PORTAL_LINKS, links.withGate(new PortalLinks.Gate(bottomLeft, width, height)));
        return rect;
    }

    private static void build(ServerLevel earth, PortalShape.Rect rect) {
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockState frame = ModBlocks.PORTAL_FRAME.get().defaultBlockState();
        BlockState floor = ModBlocks.ASHEN_SLAG.get().defaultBlockState();
        long seed = earth.getSeed();
        for (int across = -1; across <= rect.width(); across++) {
            BlockPos column = rect.at(across, -1);
            // Fill any dip under the gate so it does not float, and clear any hill inside it.
            for (int y = RuinedTerrain.surfaceY(seed, column.getX(), column.getZ()) + 1; y < column.getY(); y++) {
                earth.setBlock(new BlockPos(column.getX(), y, column.getZ()), floor, Block.UPDATE_CLIENTS);
            }
            for (int up = -1; up <= rect.height(); up++) {
                boolean interior = across >= 0 && across < rect.width() && up >= 0 && up < rect.height();
                boolean corner = (across == -1 || across == rect.width()) && (up == -1 || up == rect.height());
                if (interior) {
                    earth.setBlock(rect.at(across, up), air, Block.UPDATE_CLIENTS);
                } else if (!corner) {
                    earth.setBlock(rect.at(across, up), frame, Block.UPDATE_CLIENTS);
                }
            }
        }
        AtmosphericPortals.tryLight(earth, rect.at(0, -1));
    }
}
