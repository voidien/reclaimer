package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player Replicator state: the job queue (first job is the one being worked on), the output buffer for
 * finished items that didn't fit in the inventory, and the milestones reached in order (which is also the log).
 * Immutable; every change produces a new instance.
 */
public record ReplicatorData(List<ReplicatorJob> jobs, List<ItemStack> buffer, List<Milestone> unlocked) {
    public static final int MAX_JOBS = 5;
    public static final int BUFFER_SLOTS = 9;

    public static final ReplicatorData EMPTY = new ReplicatorData(List.of(), List.of(), List.of());

    public static final Codec<ReplicatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReplicatorJob.CODEC.listOf().optionalFieldOf("jobs", List.of()).forGetter(ReplicatorData::jobs),
            ItemStack.CODEC.listOf().optionalFieldOf("buffer", List.of()).forGetter(ReplicatorData::buffer),
            Milestone.CODEC.listOf().optionalFieldOf("unlocked", List.of()).forGetter(ReplicatorData::unlocked)
    ).apply(instance, ReplicatorData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorData> STREAM_CODEC = StreamCodec.composite(
            ReplicatorJob.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::jobs,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::buffer,
            Milestone.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::unlocked,
            ReplicatorData::new);

    /**
     * Client hook called when synced data arrives, with the previous value (null on the initial sync). The client
     * entrypoint installs it to toast new milestones; common code never references client classes.
     */
    public static volatile BiConsumer<ReplicatorData, ReplicatorData> clientSyncListener = (previous, current) -> {};

    public ReplicatorData {
        jobs = List.copyOf(jobs);
        buffer = List.copyOf(buffer);
        unlocked = List.copyOf(unlocked);
    }

    public boolean isUnlocked(Milestone milestone) {
        return unlocked.contains(milestone);
    }

    public boolean queueFull() {
        return jobs.size() >= MAX_JOBS;
    }

    @Nullable
    public ReplicatorJob current() {
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    public ReplicatorData withJobs(List<ReplicatorJob> newJobs) {
        return new ReplicatorData(newJobs, buffer, unlocked);
    }

    public ReplicatorData withBuffer(List<ItemStack> newBuffer) {
        return new ReplicatorData(jobs, newBuffer, unlocked);
    }

    public ReplicatorData withUnlocked(Milestone milestone) {
        if (unlocked.contains(milestone)) {
            return this;
        }
        List<Milestone> list = new ArrayList<>(unlocked);
        list.add(milestone);
        return new ReplicatorData(jobs, buffer, list);
    }

    /** Milestones present in {@code current} but not in {@code previous}, in unlock order. */
    public static List<Milestone> newlyUnlocked(@Nullable ReplicatorData previous, ReplicatorData current) {
        if (previous == null) {
            return List.of();
        }
        return current.unlocked().stream().filter(m -> !previous.unlocked().contains(m)).toList();
    }
}
