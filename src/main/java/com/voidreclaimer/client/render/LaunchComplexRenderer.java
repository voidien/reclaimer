package com.voidreclaimer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import com.voidreclaimer.launch.LaunchPad;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws the whole launch complex from its controller while it is assembled: the model's pad is centred on its origin,
 * so after GeoBlockRenderer's turn to the controller's facing the model is moved {@link LaunchPad#HALF} blocks forward
 * onto the pad's centre. The complex is far bigger than its block, so it is rendered as a global block entity (no
 * frustum culling by section) up to 192 blocks away.
 */
public class LaunchComplexRenderer extends GeoBlockRenderer<LaunchControllerBlockEntity> {
    public LaunchComplexRenderer(BlockEntityRendererProvider.Context context) {
        super(new LaunchComplexModel());
    }

    @Override
    public void render(LaunchControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!controller.assembled()) {
            return;
        }
        super.render(controller, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    /** After the facing turn the model's -Z points the controller's way, so this moves it onto the pad. */
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        super.rotateBlock(facing, poseStack);
        poseStack.translate(0, 0, -LaunchPad.HALF);
    }

    @Override
    public boolean shouldRenderOffScreen(LaunchControllerBlockEntity controller) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }
}
