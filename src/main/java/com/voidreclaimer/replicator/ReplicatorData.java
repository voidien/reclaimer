package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.registry.ModCodecs;
import net.minecraft.resources.ResourceKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player Replicator state: the job queue (first job is the one being worked on), the output buffer for
 * finished items that didn't fit in the inventory, the milestones reached in order (which is also the log), and
 * progress counters that milestones look at (blocks placed, and the ids of the items the player has ever held).
 * Immutable; every change produces a new instance.
 */
public record ReplicatorData(List<ReplicatorJob> jobs, List<ItemStack> buffer, List<ResourceKey<Milestone>> unlocked, int blocksPlaced,
                             List<ResourceLocation> itemsMade, List<ResourceLocation> dimensionsVisited) {
    public static final int MAX_JOBS = 5;
    public static final int BUFFER_SLOTS = 9;

    public static final ReplicatorData EMPTY = new ReplicatorData(List.of(), List.of(), List.of(), 0, List.of(), List.of());

    public static final Codec<ReplicatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // Lenient: a field that no longer parses (e.g. an item from a removed mod) resets instead of failing the load. Jobs and the
            // buffer hold the player's own things, so one bad entry costs that entry and not the list.
            ModCodecs.eachOnItsOwn(ReplicatorJob.CODEC, "a Replicator job").lenientOptionalFieldOf("jobs", List.of()).forGetter(ReplicatorData::jobs),
            ModCodecs.eachOnItsOwn(ItemStack.CODEC, "an item in the Replicator's output").lenientOptionalFieldOf("buffer", List.of()).forGetter(ReplicatorData::buffer),
            ModCodecs.key(VoidReclaimerRegistries.MILESTONE).listOf().lenientOptionalFieldOf("unlocked", List.of()).forGetter(ReplicatorData::unlocked),
            Codec.INT.lenientOptionalFieldOf("blocks_placed", 0).forGetter(ReplicatorData::blocksPlaced),
            ModCodecs.ID.listOf().lenientOptionalFieldOf("items_made", List.of()).forGetter(ReplicatorData::itemsMade),
            ModCodecs.ID.listOf().lenientOptionalFieldOf("dimensions_visited", List.of()).forGetter(ReplicatorData::dimensionsVisited)
    ).apply(instance, ReplicatorData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorData> STREAM_CODEC = StreamCodec.composite(
            ReplicatorJob.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::jobs,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::buffer,
            ResourceKey.streamCodec(VoidReclaimerRegistries.MILESTONE).apply(ByteBufCodecs.list()), ReplicatorData::unlocked,
            ByteBufCodecs.VAR_INT, ReplicatorData::blocksPlaced,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::itemsMade,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorData::dimensionsVisited,
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
        itemsMade = List.copyOf(itemsMade);
        dimensionsVisited = List.copyOf(dimensionsVisited);
    }

    public boolean isUnlocked(ResourceKey<Milestone> milestone) {
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
        return new ReplicatorData(newJobs, buffer, unlocked, blocksPlaced, itemsMade, dimensionsVisited);
    }

    public ReplicatorData withBuffer(List<ItemStack> newBuffer) {
        return new ReplicatorData(jobs, newBuffer, unlocked, blocksPlaced, itemsMade, dimensionsVisited);
    }

    public ReplicatorData withUnlocked(ResourceKey<Milestone> milestone) {
        if (unlocked.contains(milestone)) {
            return this;
        }
        List<ResourceKey<Milestone>> list = new ArrayList<>(unlocked);
        list.add(milestone);
        return new ReplicatorData(jobs, buffer, list, blocksPlaced, itemsMade, dimensionsVisited);
    }

    public ReplicatorData withoutUnlocked(ResourceKey<Milestone> milestone) {
        if (!unlocked.contains(milestone)) {
            return this;
        }
        List<ResourceKey<Milestone>> list = new ArrayList<>(unlocked);
        list.remove(milestone);
        return new ReplicatorData(jobs, buffer, list, blocksPlaced, itemsMade, dimensionsVisited);
    }

    public ReplicatorData withBlocksPlaced(int count) {
        return new ReplicatorData(jobs, buffer, unlocked, count, itemsMade, dimensionsVisited);
    }

    /** Remembers that the player has had the item {@code id}; a no-op when already recorded. */
    public ReplicatorData withDimensionVisited(ResourceLocation id) {
        if (dimensionsVisited.contains(id)) {
            return this;
        }
        List<ResourceLocation> list = new ArrayList<>(dimensionsVisited);
        list.add(id);
        return new ReplicatorData(jobs, buffer, unlocked, blocksPlaced, itemsMade, list);
    }

    public ReplicatorData withItemMade(ResourceLocation id) {
        if (itemsMade.contains(id)) {
            return this;
        }
        List<ResourceLocation> list = new ArrayList<>(itemsMade);
        list.add(id);
        return new ReplicatorData(jobs, buffer, unlocked, blocksPlaced, list, dimensionsVisited);
    }

    /** Milestones present in {@code current} but not in {@code previous}, in unlock order. */
    public static List<ResourceKey<Milestone>> newlyUnlocked(@Nullable ReplicatorData previous, ReplicatorData current) {
        if (previous == null) {
            return List.of();
        }
        return current.unlocked().stream().filter(m -> !previous.unlocked().contains(m)).toList();
    }
}
