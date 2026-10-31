package com.voidreclaimer.client.render.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.link.BodyHost;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Draws an empty body with the same renderer as a player's, so a shell looks just as its wearer did. */
public final class BodyHostRenderer extends EntityRenderer<BodyHost> {
    public BodyHostRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BodyHost host, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BodyRenderer.get().render(host, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** Never read: {@link BodyRenderer} picks the textures. */
    @Override
    public ResourceLocation getTextureLocation(BodyHost host) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
