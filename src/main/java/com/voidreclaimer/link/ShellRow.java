package com.voidreclaimer.link;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One line of the picker.
 *
 * @param shell     the shell's entity id
 * @param number    its place on the player's list, from 1, which stays as it is when another shell goes
 * @param dimension where it was last seen
 * @param distance  blocks from the player, or -1 in another dimension
 * @param status    {@code ready}, {@code out_of_range}, {@code wreck} or {@code old_copy}
 * @param strength  the signal there, 0 to 1
 */
public record ShellRow(UUID shell, int number, String dimension, double distance, String status, float strength) {
    public static final String READY = "ready";
    public static final String OUT_OF_RANGE = "out_of_range";
    public static final String WRECK = "wreck";
    /** An older copy stands refused, which only matters if the newer one is lost: see {@link ShellRecovery}. */
    public static final String OLD_COPY = "old_copy";
    private static final int NOT_HERE = -1;

    public static final StreamCodec<ByteBuf, ShellRow> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShellRow::shell,
            ByteBufCodecs.VAR_INT, ShellRow::number,
            ByteBufCodecs.STRING_UTF8, ShellRow::dimension,
            ByteBufCodecs.DOUBLE, ShellRow::distance,
            ByteBufCodecs.STRING_UTF8, ShellRow::status,
            ByteBufCodecs.FLOAT, ShellRow::strength,
            ShellRow::new);

    static ShellRow of(LinkState.KnownShell known, int number, LinkState.Place here, RelayGraph.Reading reading) {
        boolean sameDimension = known.at().dimension() == here.dimension();
        String status = known.wrecked() ? WRECK : known.oldCopy().isPresent() ? OLD_COPY : reading.reachable() ? READY : OUT_OF_RANGE;
        return new ShellRow(known.shell(), number, known.at().dimension().location().toString(),
                sameDimension ? known.at().position().distanceTo(here.position()) : NOT_HERE, status, (float) reading.strength());
    }

    public boolean ready() {
        return READY.equals(status);
    }

    public boolean recoverable() {
        return OLD_COPY.equals(status);
    }

    public boolean sameDimension() {
        return distance != NOT_HERE;
    }
}
