package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Progression milestones. Reaching one unlocks the Replicator recipes that name it and adds a first-person log
 * entry from the AI. Criteria are pure functions of player state so they can be unit tested.
 */
public enum Milestone implements StringRepresentable {
    /** Granted on first login: the AI comes online. */
    AWAKENING("awakening", context -> true),
    /** Any dust collected. */
    FIRST_DUST("first_dust", context -> context.kindsHeld() > 0),
    /** Placed {@link #STRUCTURE_BLOCKS} blocks. */
    FIRST_STRUCTURE("first_structure", context -> context.blocksPlaced() >= Milestone.STRUCTURE_BLOCKS),
    /** Holding three different kinds of dust at once. */
    DIVERSIFIED("diversified", context -> context.kindsHeld() >= 3),
    /** Any one dust kind filled to capacity: the drone outgrows its storage. Unlocks Mk II parts and the Mech frame. */
    EXPANSION("expansion", MilestoneContext::anyKindFull);

    /** Blocks to place for {@link #FIRST_STRUCTURE}. */
    public static final int STRUCTURE_BLOCKS = 10;

    public static final Codec<Milestone> CODEC = StringRepresentable.fromEnum(Milestone::values);
    private static final IntFunction<Milestone> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Milestone> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;
    private final Predicate<MilestoneContext> criterion;

    Milestone(String name, Predicate<MilestoneContext> criterion) {
        this.name = name;
        this.criterion = criterion;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isReached(MilestoneContext context) {
        return criterion.test(context);
    }

    /** Short name, e.g. for the "locked" hint. */
    public Component title() {
        return Component.translatable("milestone.void_reclaimer." + name);
    }

    /** What to do to reach it, shown on locked recipes. */
    public Component hint() {
        return Component.translatable("milestone.void_reclaimer." + name + ".hint");
    }

    /** The AI's log entry written when the milestone is reached. */
    public Component logTitle() {
        return Component.translatable("log.void_reclaimer." + name + ".title");
    }

    public Component logText() {
        return Component.translatable("log.void_reclaimer." + name + ".text");
    }
}
