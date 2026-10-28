package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.voidreclaimer.dust.DustData;
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
    AWAKENING("awakening", dust -> true),
    /** Any dust collected. */
    FIRST_DUST("first_dust", dust -> dust.metallic() > 0f || dust.silicate() > 0f || dust.ice() > 0f || dust.carbon() > 0f);

    public static final Codec<Milestone> CODEC = StringRepresentable.fromEnum(Milestone::values);
    private static final IntFunction<Milestone> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Milestone> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;
    private final Predicate<DustData> criterion;

    Milestone(String name, Predicate<DustData> criterion) {
        this.name = name;
        this.criterion = criterion;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isReached(DustData dust) {
        return criterion.test(dust);
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
