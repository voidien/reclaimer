package com.voidreclaimer.api.event;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Too much drilling is about to split a rock, on the game bus and the server side. Cancel it and the rock holds, until the next hole tries again. */
public final class RockFractureEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos hole;
    private final List<BlockPos> piece;

    public RockFractureEvent(ServerLevel level, BlockPos hole, List<BlockPos> piece) {
        this.level = level;
        this.hole = hole;
        this.piece = List.copyOf(piece);
    }

    public ServerLevel getLevel() {
        return level;
    }

    /** The last block drilled out. */
    public BlockPos getHole() {
        return hole;
    }

    /** The blocks that would come away. */
    public List<BlockPos> getPiece() {
        return piece;
    }
}
