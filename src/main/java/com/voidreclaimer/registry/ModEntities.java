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

    /** A helper drone: a small flying worker (the mining probe model at half size). */
    public static final Supplier<EntityType<com.voidreclaimer.helper.HelperDrone>> HELPER_DRONE = ENTITY_TYPES.register("helper_drone",
            () -> EntityType.Builder.<com.voidreclaimer.helper.HelperDrone>of(com.voidreclaimer.helper.HelperDrone::new, MobCategory.MISC)
                    .sized(0.9f, 0.9f).fireImmune().clientTrackingRange(10).updateInterval(2)
                    .build(VoidReclaimer.id("helper_drone").toString()));

    /** A body with nobody in it (see {@code link.BodyHost}): the hit box is a player's, scaled by the body like a player's. */
    public static final Supplier<EntityType<com.voidreclaimer.link.BodyHost>> BODY_HOST = ENTITY_TYPES.register("body_host",
            () -> EntityType.Builder.<com.voidreclaimer.link.BodyHost>of(com.voidreclaimer.link.BodyHost::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).noSummon().fireImmune().clientTrackingRange(10).updateInterval(2)
                    .build(VoidReclaimer.id("body_host").toString()));

    /** A rock in a debris storm: never saved, and it ends at the first thing it meets. */
    public static final Supplier<EntityType<com.voidreclaimer.weather.DebrisRock>> DEBRIS_ROCK = ENTITY_TYPES.register("debris_rock",
            () -> EntityType.Builder.<com.voidreclaimer.weather.DebrisRock>of(com.voidreclaimer.weather.DebrisRock::new, MobCategory.MISC)
                    .sized(0.6f, 0.6f).noSave().noSummon().clientTrackingRange(8).updateInterval(1)
                    .build(VoidReclaimer.id("debris_rock").toString()));

    private ModEntities() {}
}
