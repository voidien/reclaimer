package com.voidreclaimer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.starship.TestStarship;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws the {@link TestStarship} from {@code geo/entity/starship.geo.json}. The model is as long as the Ship body's
 * rig, so it is drawn at the Ship size class's scale (from the synced size class registry) to match a Ship-class body.
 */
public class TestStarshipRenderer extends GeoEntityRenderer<TestStarship> {
    public TestStarshipRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(VoidReclaimer.id("starship")));
        shadowRadius = 0f;
    }

    @Override
    public void render(TestStarship ship, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        double scale = ship.level().registryAccess().registry(VoidReclaimerRegistries.SIZE_CLASS)
                .flatMap(r -> r.getOptional(BuiltinBodies.SHIP_SIZE)).map(SizeClass::scale).orElse(3.0);
        withScale((float) scale);
        super.render(ship, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** GeckoLib only turns living entities; this one is a plain entity, so use its own yaw. */
    @Override
    protected void applyRotations(TestStarship ship, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(ship, poseStack, ageInTicks, Mth.rotLerp(partialTick, ship.yRotO, ship.getYRot()), partialTick, nativeScale);
    }
}
