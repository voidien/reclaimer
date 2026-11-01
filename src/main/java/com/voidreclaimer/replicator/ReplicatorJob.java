package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * One queued Replicator job. Stores its result and the dust actually paid, so it completes (or refunds) correctly
 * even if the recipe changes or disappears with a datapack reload.
 *
 * @param recipe   recipe id (display and bookkeeping)
 * @param results  items produced on completion
 * @param paid     dust paid when queued; refunded on cancel
 * @param items    items taken from the inventory when queued; given back on cancel
 * @param progress ticks of work done
 * @param total    ticks of work needed
 */
public record ReplicatorJob(ResourceLocation recipe, List<ItemStack> results, DustCost paid, List<ItemStack> items, int progress, int total) {
    public static final Codec<ReplicatorJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(ReplicatorJob::recipe),
            // Jobs saved before multi-output recipes have a single "result".
            ItemStack.CODEC.lenientOptionalFieldOf("result").forGetter(job -> Optional.empty()),
            // A job with a result that has gone stays in the queue, so a cancel can still give back what it took.
            ModCodecs.eachOnItsOwn(ItemStack.CODEC, "a Replicator job's result").optionalFieldOf("results", List.of()).forGetter(ReplicatorJob::results),
            DustCost.CODEC.optionalFieldOf("paid", DustCost.NONE).forGetter(ReplicatorJob::paid),
            ModCodecs.eachOnItsOwn(ItemStack.CODEC, "an item a Replicator job took").optionalFieldOf("items", List.of()).forGetter(ReplicatorJob::items),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(ReplicatorJob::progress),
            Codec.INT.optionalFieldOf("total", 1).forGetter(ReplicatorJob::total)
    ).apply(instance, (recipe, single, results, paid, items, progress, total) -> new ReplicatorJob(recipe,
            results.isEmpty() ? single.map(List::of).orElse(List.of()) : results, paid, items, progress, total)));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorJob> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ReplicatorJob::recipe,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorJob::results,
            DustCost.STREAM_CODEC, ReplicatorJob::paid,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorJob::items,
            ByteBufCodecs.VAR_INT, ReplicatorJob::progress,
            ByteBufCodecs.VAR_INT, ReplicatorJob::total,
            ReplicatorJob::new);

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
        return new ReplicatorJob(recipe, results, paid, items, Math.min(total, progress + steps), total);
    }
}
