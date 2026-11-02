package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModCodecs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One queued Replicator job. Stores its result and the dust actually paid, so it completes (or refunds) correctly
 * even if the recipe changes or disappears with a datapack reload.
 *
 * @param recipe   recipe id (display and bookkeeping)
 * @param results  items produced on completion
 * @param paid     dust paid when queued; refunded on cancel
 * @param items    items taken from the inventory when queued; given back on cancel
 * @param paidBy   the shell that paid, or empty for the main body: only that body can cancel a job that took items
 * @param progress ticks of work done
 * @param total    ticks of work needed
 */
public record ReplicatorJob(ResourceLocation recipe, List<ItemStack> results, DustCost paid, List<ItemStack> items, Optional<UUID> paidBy, int progress, int total) {
    public static final Codec<ReplicatorJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(ReplicatorJob::recipe),
            // Jobs saved before multi-output recipes have a single "result".
            ItemStack.CODEC.lenientOptionalFieldOf("result").forGetter(job -> Optional.empty()),
            // A job with a result that has gone stays in the queue, so a cancel can still give back what it took.
            ModCodecs.eachOnItsOwn(ItemStack.CODEC, "a Replicator job's result").optionalFieldOf("results", List.of()).forGetter(ReplicatorJob::results),
            DustCost.CODEC.optionalFieldOf("paid", DustCost.NONE).forGetter(ReplicatorJob::paid),
            ModCodecs.eachOnItsOwn(ItemStack.CODEC, "an item a Replicator job took").optionalFieldOf("items", List.of()).forGetter(ReplicatorJob::items),
            UUIDUtil.CODEC.optionalFieldOf("paid_by").forGetter(ReplicatorJob::paidBy),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(ReplicatorJob::progress),
            Codec.INT.optionalFieldOf("total", 1).forGetter(ReplicatorJob::total)
    ).apply(instance, (recipe, single, results, paid, items, paidBy, progress, total) -> new ReplicatorJob(recipe,
            results.isEmpty() ? single.map(List::of).orElse(List.of()) : results, paid, items, paidBy, progress, total)));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<io.netty.buffer.ByteBuf, Optional<UUID>> BODY = ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC);

    /** Written by hand: {@code StreamCodec.composite} stops at six fields. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorJob> STREAM_CODEC = StreamCodec.of(
            (buf, job) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, job.recipe);
                STACKS.encode(buf, job.results);
                DustCost.STREAM_CODEC.encode(buf, job.paid);
                STACKS.encode(buf, job.items);
                BODY.encode(buf, job.paidBy);
                ByteBufCodecs.VAR_INT.encode(buf, job.progress);
                ByteBufCodecs.VAR_INT.encode(buf, job.total);
            },
            buf -> new ReplicatorJob(ResourceLocation.STREAM_CODEC.decode(buf), STACKS.decode(buf), DustCost.STREAM_CODEC.decode(buf), STACKS.decode(buf),
                    BODY.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    public ReplicatorJob {
        results = results.stream().map(ItemStack::copy).toList();
        items = items.stream().map(ItemStack::copy).toList();
    }

    /** The first result (the one shown), or empty. */
    public ItemStack result() {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    public boolean isDone() {
        return progress >= total;
    }

    public float fraction() {
        return total <= 0 ? 1f : Math.min(1f, (float) progress / total);
    }

    ReplicatorJob advanced() {
        return advanced(1);
    }

    ReplicatorJob advanced(int steps) {
        return new ReplicatorJob(recipe, results, paid, items, paidBy, Math.min(total, progress + steps), total);
    }
}
