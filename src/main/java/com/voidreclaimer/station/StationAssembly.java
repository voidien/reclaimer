package com.voidreclaimer.station;

import com.voidreclaimer.Config;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Turns the blocks round a Station Core into a station (a Sable ship) and sets a station down again as blocks.
 *
 * <p>Assembly is Sable's own: a flood fill over the 18 face-and-edge neighbours from the core
 * ({@code SubLevelAssemblyHelper.gatherConnectedBlocks}), then {@code assembleBlocks}, which moves blocks, block
 * entities and hanging entities into a new plot. Setting down has no Sable API; Aeronautics' "Simulated" jar has one
 * ({@code SimAssemblyHelper.disassembleSubLevel}) which moves blocks turned by whole quarter turns about Y only, so the
 * ship must be level and is snapped to the nearest quarter turn. The emptied ship is removed by Sable on its next tick.
 */
public final class StationAssembly {
    /** What a call did, for messages and the peripheral. */
    public enum Result { ASSEMBLED, DISASSEMBLED, ALREADY_ASSEMBLED, NOT_ASSEMBLED, NOTHING, TOO_BIG, NOT_LEVEL, BLOCKED, FAILED }

    /** The station's mark in Sable's per-ship user data. */
    static final String STATION_TAG = "void_reclaimer_station";

    private StationAssembly() {}

    /** Sneak-use at the core: assemble if the core sits in the world, set down if it is a station. Tells the player. */
    public static void toggle(StationCoreBlockEntity core, Player player) {
        Result result = core.ship() == null ? assemble(core) : disassemble(core);
        player.displayClientMessage(message(core, result), true);
    }

    /** Everything connected to the core becomes one station named after it. */
    public static Result assemble(StationCoreBlockEntity core) {
        if (!(core.getLevel() instanceof ServerLevel level)) {
            return Result.FAILED;
        }
        if (core.ship() != null) {
            return Result.ALREADY_ASSEMBLED;
        }
        BlockPos pos = core.getBlockPos();
        SubLevelAssemblyHelper.GatherResult gathered = SubLevelAssemblyHelper.gatherConnectedBlocks(pos, level, Config.STATION_MAX_BLOCKS.get(), null);
        switch (gathered.assemblyState()) {
            case NO_BLOCKS -> { return Result.NOTHING; }
            case TOO_MANY_BLOCKS -> { return Result.TOO_BIG; }
            default -> { }
        }
        if (gathered.blocks() == null || gathered.blocks().size() < 2) {
            return Result.NOTHING;
        }
        String name = core.stationName();
        ServerSubLevel ship = SubLevelAssemblyHelper.assembleBlocks(level, pos, gathered.blocks(), gathered.boundingBox());
        if (ship == null || ship.getMassTracker() == null || ship.getMassTracker().isInvalid()) {
            return Result.FAILED;
        }
        ship.setName(name);
        CompoundTag data = ship.getUserDataTag() == null ? new CompoundTag() : ship.getUserDataTag();
        data.putBoolean(STATION_TAG, true);
        data.putUUID("station_core", core.id());
        ship.setUserDataTag(data);
        return Result.ASSEMBLED;
    }

    /** The station's blocks go back into the world where the station is, turned to the nearest quarter turn. */
    public static Result disassemble(StationCoreBlockEntity core) {
        if (!(core.getLevel() instanceof ServerLevel level)) {
            return Result.FAILED;
        }
        ServerSubLevel ship = core.ship();
        if (ship == null) {
            return Result.NOT_ASSEMBLED;
        }
        if (!StationMath.isLevel(ship.logicalPose().orientation(), StationMath.LEVEL_TOLERANCE_DEGREES)) {
            return Result.NOT_LEVEL;
        }
        int turns = StationMath.quarterTurns(ship.logicalPose().orientation());
        Rotation rotation = StationMath.blockRotation(turns);
        BlockPos anchor = core.getBlockPos();
        BlockPos target = BlockPos.containing(ship.logicalPose().transformPosition(Vec3.atCenterOf(anchor)));
        List<BlockPos> blocks = blocksOf(level, ship);
        if (blocks.isEmpty()) {
            return Result.NOTHING;
        }
        SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(anchor, target, turns, rotation, level);
        for (BlockPos block : blocks) {
            BlockPos where = transform.apply(block);
            BlockState there = level.getBlockState(where);
            if (!there.isAir() && !there.canBeReplaced()) {
                return Result.BLOCKED;
            }
        }
        SimAssemblyHelper.disassembleSubLevel(level, ship, anchor, target, rotation, true);
        return Result.DISASSEMBLED;
    }

    /** Every non-air block of the ship, in its plot's coordinates. */
    static List<BlockPos> blocksOf(ServerLevel level, ServerSubLevel ship) {
        List<BlockPos> out = new ArrayList<>();
        for (PlotChunkHolder holder : ship.getPlot().getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            // The holder's box is chunk-local in x and z (as Simulated reads it); y is absolute.
            var box = holder.getBoundingBox();
            int originX = holder.getPos().getMinBlockX(), originZ = holder.getPos().getMinBlockZ();
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        BlockPos pos = new BlockPos(originX + x, y, originZ + z);
                        if (!chunk.getBlockState(pos).isAir()) {
                            out.add(pos);
                        }
                    }
                }
            }
        }
        return out;
    }

    /** The player's message for {@code result}. */
    public static Component message(StationCoreBlockEntity core, Result result) {
        String key = "message.void_reclaimer.station." + result.name().toLowerCase(Locale.ROOT);
        ChatFormatting colour = result == Result.ASSEMBLED || result == Result.DISASSEMBLED ? ChatFormatting.AQUA : ChatFormatting.RED;
        return switch (result) {
            case ASSEMBLED -> Component.translatable(key, core.stationName()).withStyle(colour);
            case DISASSEMBLED -> Component.translatable(key, core.stationName()).withStyle(colour);
            case TOO_BIG -> Component.translatable(key, Config.STATION_MAX_BLOCKS.get()).withStyle(colour);
            default -> Component.translatable(key).withStyle(colour);
        };
    }
}
