package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.starship.TestStarship;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, VoidReclaimer.MOD_ID);

    /** Placeholder starship for model and animation tests; its hitbox covers the hull at the Ship class's 3.0 scale. */
    public static final Supplier<EntityType<TestStarship>> TEST_STARSHIP = ENTITY_TYPES.register("test_starship",
            () -> EntityType.Builder.<TestStarship>of(TestStarship::new, MobCategory.MISC)
                    .sized(8.5f, 5f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .build(VoidReclaimer.id("test_starship").toString()));

    /** The invisible seat a pilot rides at the Helm. */
    public static final Supplier<EntityType<com.voidreclaimer.ship.HelmSeat>> HELM_SEAT = ENTITY_TYPES.register("helm_seat",
            () -> EntityType.Builder.<com.voidreclaimer.ship.HelmSeat>of(com.voidreclaimer.ship.HelmSeat::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).noSummon().clientTrackingRange(8).updateInterval(20)
                    .build(VoidReclaimer.id("helm_seat").toString()));

    private ModEntities() {}
}
