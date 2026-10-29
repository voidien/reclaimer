package com.voidreclaimer.replicator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.MilestoneContext;
import com.voidreclaimer.api.milestone.MilestoneCriterion;
import com.voidreclaimer.registry.ModCodecs;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The built-in milestone criterion types. */
public final class MilestoneCriteria {
    public static final DeferredRegister<MapCodec<? extends MilestoneCriterion>> TYPES =
            DeferredRegister.create(VoidReclaimerRegistries.MILESTONE_CRITERION_TYPE, VoidReclaimer.MOD_ID);

    static {
        TYPES.register("always", () -> Always.CODEC);
        TYPES.register("dust_collected", () -> DustCollected.CODEC);
        TYPES.register("dust_kinds_held", () -> DustKindsHeld.CODEC);
        TYPES.register("blocks_placed", () -> BlocksPlaced.CODEC);
        TYPES.register("dust_full", () -> DustFull.CODEC);
        TYPES.register("dyson_tier", () -> DysonTier.CODEC);
        TYPES.register("items_made", () -> ItemsMade.CODEC);
    }

    private MilestoneCriteria() {}

    /** Reached immediately (the AI comes online). */
    public record Always() implements MilestoneCriterion {
        public static final Always INSTANCE = new Always();
        public static final MapCodec<Always> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean isReached(MilestoneContext context) {
            return true;
        }

        @Override
        public MapCodec<Always> codec() {
            return CODEC;
        }
    }

    /** Holding at least {@code amount} dust of {@code kind}, or of all kinds together when no kind is given. */
    public record DustCollected(Optional<ResourceLocation> kind, float amount) implements MilestoneCriterion {
        public static final MapCodec<DustCollected> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ModCodecs.ID.optionalFieldOf("kind").forGetter(DustCollected::kind),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("amount").forGetter(DustCollected::amount)
        ).apply(instance, DustCollected::new));

        @Override
        public boolean isReached(MilestoneContext context) {
            float held = kind.map(k -> context.dust().get(k)).orElseGet(() -> context.dust().total());
            return held + 1.0E-4f >= amount;
        }

        @Override
        public MapCodec<DustCollected> codec() {
            return CODEC;
        }
    }

    /** Holding some dust of at least {@code count} different kinds at once. */
    public record DustKindsHeld(int count) implements MilestoneCriterion {
        public static final MapCodec<DustKindsHeld> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(DustKindsHeld::count)
        ).apply(instance, DustKindsHeld::new));

        @Override
        public boolean isReached(MilestoneContext context) {
            return context.dust().kindsHeld() >= count;
        }

        @Override
        public MapCodec<DustKindsHeld> codec() {
            return CODEC;
        }
    }

    /** Having placed at least {@code count} blocks (by hand or with the Nanite Swarm). */
    public record BlocksPlaced(int count) implements MilestoneCriterion {
        public static final MapCodec<BlocksPlaced> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(BlocksPlaced::count)
        ).apply(instance, BlocksPlaced::new));

        @Override
        public boolean isReached(MilestoneContext context) {
            return context.blocksPlaced() >= count;
        }

        @Override
        public MapCodec<BlocksPlaced> codec() {
            return CODEC;
        }
    }

    /** {@code kind} (or any kind when none is given) filled to capacity. */
    public record DustFull(Optional<ResourceLocation> kind) implements MilestoneCriterion {
        public static final MapCodec<DustFull> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ModCodecs.ID.optionalFieldOf("kind").forGetter(DustFull::kind)
        ).apply(instance, DustFull::new));

        @Override
        public boolean isReached(MilestoneContext context) {
            return kind.map(k -> context.dust().get(k) >= context.dustCapacity() - 1.0E-3f)
                    .orElseGet(() -> context.dust().anyKindFull(context.dustCapacity()));
        }

        @Override
        public MapCodec<DustFull> codec() {
            return CODEC;
        }
    }

    /** The Dyson Sphere has reached skybox tier {@code tier} (1 to 4), for everyone in the world. */
    public record DysonTier(int tier) implements MilestoneCriterion {
        public static final MapCodec<DysonTier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.intRange(1, 4).fieldOf("tier").forGetter(DysonTier::tier)
        ).apply(instance, DysonTier::new));

        @Override
        public boolean isReached(MilestoneContext context) {
            return context.dysonTier() >= tier;
        }

        @Override
        public MapCodec<DysonTier> codec() {
            return CODEC;
        }
    }

    /**
     * The player has held every item in {@code items} at some point (made, crafted, taken from a machine or picked up; the
     * Replicator checks the inventory about once a second).
     */
    public record ItemsMade(java.util.List<ResourceLocation> items) implements MilestoneCriterion {
        public static final MapCodec<ItemsMade> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.nonEmptyList(ModCodecs.ID.listOf()).fieldOf("items").forGetter(ItemsMade::items)
        ).apply(instance, ItemsMade::new));

        @Override
        public boolean isReached(MilestoneContext context) {
            return context.itemsMade().containsAll(items);
        }

        @Override
        public MapCodec<ItemsMade> codec() {
            return CODEC;
        }
    }
}
