package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModCodecs;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A body section (the {@code void_reclaimer:section} data component, a default component on section items). Stats of
 * all installed sections add up; a missing section simply doesn't contribute.
 *
 * @param socketType     what sockets it fits ({@link Socket#accepts()})
 * @param tier           the body's core module must be at least this tier
 * @param mass           adds to body mass
 * @param massBudget     mass the body carries without penalty
 * @param maxHealth      max health in half-hearts
 * @param reach          extra block and entity interaction range
 * @param dustIntake     dust collection multiplier (summed)
 * @param baseRows       main-inventory rows (beyond the hotbar) without storage modules
 * @param stability      footing on solid ground (Ruined Earth)
 * @param turnRate       rotation help for future 6-DOF handling
 * @param wearResistance 0..1, how much slower the section wears
 * @param mounts         module mounts it offers, per kind
 * @param chassis        set only on core sections: the layout the body takes when it is installed
 */
public record SectionStats(ResourceLocation socketType, int tier, float mass, float massBudget, float maxHealth, float reach,
                           float dustIntake, int baseRows, float stability, float turnRate, float wearResistance,
                           Map<ModuleKind, Integer> mounts, Optional<ResourceKey<Chassis>> chassis) {
    public static final Codec<SectionStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("socket_type").forGetter(SectionStats::socketType),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(SectionStats::tier),
            Codec.FLOAT.optionalFieldOf("mass", 0f).forGetter(SectionStats::mass),
            Codec.FLOAT.optionalFieldOf("mass_budget", 0f).forGetter(SectionStats::massBudget),
            Codec.FLOAT.optionalFieldOf("max_health", 0f).forGetter(SectionStats::maxHealth),
            Codec.FLOAT.optionalFieldOf("reach", 0f).forGetter(SectionStats::reach),
            Codec.FLOAT.optionalFieldOf("dust_intake", 0f).forGetter(SectionStats::dustIntake),
            Codec.INT.optionalFieldOf("base_rows", 0).forGetter(SectionStats::baseRows),
            Codec.FLOAT.optionalFieldOf("stability", 0f).forGetter(SectionStats::stability),
            Codec.FLOAT.optionalFieldOf("turn_rate", 0f).forGetter(SectionStats::turnRate),
            Codec.floatRange(0f, 1f).optionalFieldOf("wear_resistance", 0f).forGetter(SectionStats::wearResistance),
            Codec.unboundedMap(ModuleKind.CODEC, Codec.INT).optionalFieldOf("mounts", Map.of()).forGetter(SectionStats::mounts),
            ModCodecs.key(VoidReclaimerRegistries.CHASSIS).optionalFieldOf("chassis").forGetter(SectionStats::chassis)
    ).apply(instance, SectionStats::new));

    public static final StreamCodec<ByteBuf, SectionStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public int mounts(ModuleKind kind) {
        return mounts.getOrDefault(kind, 0);
    }

    public boolean isCore() {
        return chassis.isPresent();
    }
}
