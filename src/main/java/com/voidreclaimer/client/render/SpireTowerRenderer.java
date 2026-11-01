package com.voidreclaimer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.terraform.SpireBlockEntity;
import com.voidreclaimer.terraform.SpireTower;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws the Spire's tower from its block entity while it is assembled: the model stands on its origin, so after
 * GeoBlockRenderer's turn to the Spire's facing it is moved {@link SpireTower#HALF} blocks forward onto the base's centre.
 * Taller than its block, so it is a global block entity seen up to 256 blocks away. The model is a placeholder
 * ({@code geo/entity/spire_tower.geo.json}); art can replace it under the same name.
 */
public class SpireTowerRenderer extends GeoBlockRenderer<SpireBlockEntity> {
    public SpireTowerRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedEntityGeoModel<>(VoidReclaimer.id("spire_tower")));
    }

    @Override
    public void render(SpireBlockEntity spire, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (spire.assembled()) {
            super.render(spire, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    /** After the facing turn the model's -Z points the Spire's way, so this moves it onto the base. */
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        super.rotateBlock(facing, poseStack);
        poseStack.translate(0, 0, -SpireTower.HALF);
    }

    @Override
    public boolean shouldRenderOffScreen(SpireBlockEntity spire) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
