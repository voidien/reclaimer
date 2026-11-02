package com.voidreclaimer.replicator;

import com.voidreclaimer.dust.DustData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Pure Replicator queue rules, independent of players and the world so they can be unit tested. */
public final class ReplicatorLogic {
    private ReplicatorLogic() {}

    public enum QueueResult { QUEUED, LOCKED, QUEUE_FULL, NOT_ENOUGH_DUST, UNKNOWN_RECIPE, NOT_ENOUGH_ITEMS }

    /**
     * Whether a body can cancel a job. A job that took items gives them back to the body that paid, so they can't travel for free, and
     * only that body can cancel it. If that body has gone there is nowhere to wait for, so any body can.
     *
     * @param body      the shell doing this, or empty for the main body
     * @param bodyGone  whether a shell has gone for good
     */
    public static boolean canCancel(ReplicatorJob job, Optional<UUID> body, Predicate<UUID> bodyGone) {
        return job.items().isEmpty() || job.paidBy().equals(body) || job.paidBy().filter(bodyGone).isPresent();
    }

    /** What a body that has gone leaves: the queue and dust without its jobs, and the items those jobs took. */
    public record Lost(ReplicatorData data, DustData dust, List<ItemStack> items) {
        public static Lost nothing(ReplicatorData data, DustData dust) {
            return new Lost(data, dust, List.of());
        }
    }

    /**
     * A body has gone, so the jobs it paid for with items end. The items go where the body's other things go, and the dust comes back,
     * because dust belongs to the mind. Jobs that took no items carry on: they can be finished or cancelled by any body.
     */
    public static Lost loseBody(ReplicatorData data, DustData dust, Optional<UUID> body, float capacity) {
        List<ReplicatorJob> kept = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        DustData left = dust;
        for (ReplicatorJob job : data.jobs()) {
            if (!job.items().isEmpty() && job.paidBy().equals(body)) {
                items.addAll(job.items());
                left = job.paid().giveTo(left, capacity);
            } else {
                kept.add(job);
            }
        }
        return new Lost(items.isEmpty() ? data : data.withJobs(kept), left, items);
    }

    /** Outcome of queueing: the result code and, when queued, the new data and dust. */
    public record Queued(QueueResult result, ReplicatorData data, DustData dust) {}

    /** A job that takes no items: the rules alone, for tests. The game goes through the one below, which knows what the player holds. */
    static Queued queue(ReplicatorData data, DustData dust, ResourceLocation id, @Nullable ReplicatorRecipe recipe) {
        return queue(data, dust, id, recipe, List.of(), Optional.empty());
    }

    /** {@code items} are what the job takes from the inventory, or null when the player hasn't got them; {@code body} is who pays. */
    public static Queued queue(ReplicatorData data, DustData dust, ResourceLocation id, @Nullable ReplicatorRecipe recipe, @Nullable List<ItemStack> items,
                               Optional<UUID> body) {
        if (recipe == null) {
            return new Queued(QueueResult.UNKNOWN_RECIPE, data, dust);
        }
        if (!data.isUnlocked(recipe.milestone())) {
            return new Queued(QueueResult.LOCKED, data, dust);
        }
        if (data.queueFull()) {
            return new Queued(QueueResult.QUEUE_FULL, data, dust);
        }
        if (!recipe.cost().canAfford(dust)) {
            return new Queued(QueueResult.NOT_ENOUGH_DUST, data, dust);
        }
        if (items == null) {
            return new Queued(QueueResult.NOT_ENOUGH_ITEMS, data, dust);
        }
        List<ReplicatorJob> jobs = new ArrayList<>(data.jobs());
        jobs.add(new ReplicatorJob(id, recipe.results(), recipe.cost(), items, body, 0, recipe.ticks()));
        return new Queued(QueueResult.QUEUED, data.withJobs(jobs), recipe.cost().takeFrom(dust));
    }

    /** Removes job {@code index} and refunds what it paid. Out-of-range indexes change nothing. */
    public static Queued cancel(ReplicatorData data, DustData dust, int index, float capacity) {
        if (index < 0 || index >= data.jobs().size()) {
            return new Queued(QueueResult.UNKNOWN_RECIPE, data, dust);
        }
        List<ReplicatorJob> jobs = new ArrayList<>(data.jobs());
        ReplicatorJob removed = jobs.remove(index);
        return new Queued(QueueResult.QUEUED, data.withJobs(jobs), removed.paid().giveTo(dust, capacity));
    }

    /**
     * Moves queued job {@code from} to position {@code to} (clamped to the queue). The first job is being worked on and never
     * moves, and nothing moves in front of it, so progress is never lost. Anything invalid, or a move to the same place,
     * returns {@code data} itself.
     */
    public static ReplicatorData reorder(ReplicatorData data, int from, int to) {
        int size = data.jobs().size();
        if (from < 1 || from >= size) {
            return data;
        }
        int target = Math.max(1, Math.min(size - 1, to));
        if (target == from) {
            return data;
        }
        List<ReplicatorJob> jobs = new ArrayList<>(data.jobs());
        jobs.add(target, jobs.remove(from));
        return data.withJobs(jobs);
    }

    /**
     * Result of one work tick.
     *
     * @param data     updated data
     * @param finished items produced this tick that still need delivering (empty list when none)
     * @param worked   whether work was done (energy should be charged)
     */
    public record Tick(ReplicatorData data, List<ItemStack> finished, boolean worked) {}

    /**
     * Advances the first job by one tick when powered. A finished job is removed and its items returned for
     * delivery, unless the output buffer lacks room for all of them, in which case the finished job waits at the head
     * of the queue (no work, no energy) until there is room.
     */
    public static Tick tick(ReplicatorData data, boolean powered) {
        return tick(data, powered, 1);
    }

    /** As {@link #tick(ReplicatorData, boolean)}, making {@code steps} ticks of progress at once (a station's bonus). */
    public static Tick tick(ReplicatorData data, boolean powered, int steps) {
        ReplicatorJob job = data.current();
        if (job == null || !powered) {
            return new Tick(data, List.of(), false);
        }
        if (job.isDone()) {
            if (!hasRoom(data, job)) {
                return new Tick(data, List.of(), false);
            }
            List<ReplicatorJob> jobs = new ArrayList<>(data.jobs());
            jobs.remove(0);
            return new Tick(data.withJobs(jobs), job.results(), false);
        }
        List<ReplicatorJob> jobs = new ArrayList<>(data.jobs());
        ReplicatorJob advanced = job.advanced(Math.max(1, steps));
        jobs.set(0, advanced);
        ReplicatorData updated = data.withJobs(jobs);
        if (advanced.isDone() && hasRoom(updated, advanced)) {
            jobs.remove(0);
            return new Tick(updated.withJobs(jobs), advanced.results(), true);
        }
        return new Tick(updated, List.of(), true);
    }

    private static boolean hasRoom(ReplicatorData data, ReplicatorJob job) {
        return data.buffer().size() + job.results().size() <= ReplicatorData.BUFFER_SLOTS;
    }

    /** Puts a stack that didn't fit in the inventory into the output buffer. */
    public static ReplicatorData toBuffer(ReplicatorData data, ItemStack leftover) {
        if (leftover.isEmpty()) {
            return data;
        }
        List<ItemStack> buffer = new ArrayList<>(data.buffer());
        buffer.add(leftover.copy());
        return data.withBuffer(buffer);
    }
}
