package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * @param result   item produced on completion
 * @param paid     dust paid when queued; refunded on cancel
 * @param progress ticks of work done
 * @param total    ticks of work needed
 */
public record ReplicatorJob(ResourceLocation recipe, ItemStack result, DustCost paid, int progress, int total) {
    public static final Codec<ReplicatorJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("recipe").forGetter(ReplicatorJob::recipe),
            ItemStack.CODEC.fieldOf("result").forGetter(ReplicatorJob::result),
            DustCost.CODEC.optionalFieldOf("paid", DustCost.NONE).forGetter(ReplicatorJob::paid),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(ReplicatorJob::progress),
            Codec.INT.optionalFieldOf("total", 1).forGetter(ReplicatorJob::total)
    ).apply(instance, ReplicatorJob::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorJob> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ReplicatorJob::recipe,
            ItemStack.STREAM_CODEC, ReplicatorJob::result,
            DustCost.STREAM_CODEC, ReplicatorJob::paid,
            ByteBufCodecs.VAR_INT, ReplicatorJob::progress,
            ByteBufCodecs.VAR_INT, ReplicatorJob::total,
            ReplicatorJob::new);

    public boolean isDone() {
        return progress >= total;
    }

    public float fraction() {
        return total <= 0 ? 1f : Math.min(1f, (float) progress / total);
    }

    ReplicatorJob advanced() {
        return new ReplicatorJob(recipe, result, paid, Math.min(total, progress + 1), total);
    }
}
