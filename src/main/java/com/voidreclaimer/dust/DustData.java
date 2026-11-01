package com.voidreclaimer.dust;

import com.mojang.serialization.Codec;
import com.voidreclaimer.Config;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The AI's internal dust reserve: an amount per dust kind id. Spent by the Replicator.
 *
 * <p>Saved as {@code {"void_reclaimer:metallic": 12.5, ...}}. Older saves wrote bare names ({@code "metallic"}),
 * which {@link DustKinds#ID_CODEC} still reads. Kinds that are no longer registered are kept, not dropped.
 */
public record DustData(Map<ResourceLocation, Float> amounts) {
    public static final DustData EMPTY = new DustData(Map.of());

    public static final Codec<DustData> CODEC = amountsCodec().xmap(DustData::new, DustData::amounts);
    public static final StreamCodec<ByteBuf, DustData> STREAM_CODEC = amountsStreamCodec().map(DustData::new, DustData::amounts);

    public DustData {
        amounts = normalize(amounts);
    }

    /** Shorthand for the four built-in kinds. */
    public static DustData of(float metallic, float silicate, float ice, float carbon) {
        return new DustData(java.util.Map.of(DustKinds.METALLIC, metallic, DustKinds.SILICATE, silicate,
                DustKinds.ICE, ice, DustKinds.CARBON, carbon));
    }

    /** The value of {@code HOME_DUST_CAPACITY} when the player is in their main body. */
    public static final float NOT_LINKED = -1f;

    /**
     * Storage per kind: the config base plus the storage parts of the main body. The AI streams its dust through the network, so
     * a shell reaches the same reserve and its own storage parts do not count.
     */
    public static float capacity(Player player) {
        float home = player.getData(com.voidreclaimer.registry.ModAttachments.HOME_DUST_CAPACITY);
        float body = home >= 0f ? home : com.voidreclaimer.body.Bodies.stats(player).dustCapacity();
        return Config.DUST_CAPACITY.get().floatValue() + body;
    }

    public float get(ResourceLocation kind) {
        return amounts.getOrDefault(kind, 0f);
    }

    public DustData with(ResourceLocation kind, float amount) {
        Map<ResourceLocation, Float> copy = new HashMap<>(amounts);
        copy.put(kind, amount);
        return new DustData(copy);
    }

    /**
     * Adds dust of one kind up to {@code capacity}. A reserve already above it, as taking off a storage module leaves one,
     * stays as it is: gaining dust never lowers it. A negative amount still takes dust away.
     */
    public DustData add(ResourceLocation kind, float amount, float capacity) {
        float held = get(kind);
        return with(kind, Math.max(Math.min(capacity, held + amount), Math.min(held, held + amount)));
    }

    /** How many kinds the reserve holds any of. */
    public int kindsHeld() {
        return amounts.size();
    }

    /** Whether any one kind is at {@code capacity}. */
    public boolean anyKindFull(float capacity) {
        for (float amount : amounts.values()) {
            if (amount >= capacity - 1.0E-3f) {
                return true;
            }
        }
        return false;
    }

    public float total() {
        float total = 0f;
        for (float amount : amounts.values()) {
            total += amount;
        }
        return total;
    }

    /** Unmodifiable, sorted by id, without empty kinds. */
    static Map<ResourceLocation, Float> normalize(Map<ResourceLocation, Float> amounts) {
        Map<ResourceLocation, Float> sorted = new TreeMap<>();
        amounts.forEach((kind, amount) -> {
            if (amount != null && amount > 0f) {
                sorted.put(kind, amount);
            }
        });
        return java.util.Collections.unmodifiableMap(sorted);
    }

    static Codec<Map<ResourceLocation, Float>> amountsCodec() {
        return Codec.unboundedMap(DustKinds.ID_CODEC, Codec.FLOAT);
    }

    static StreamCodec<ByteBuf, Map<ResourceLocation, Float>> amountsStreamCodec() {
        return ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.FLOAT);
    }
}
