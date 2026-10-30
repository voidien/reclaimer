package com.voidreclaimer.client.render;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.ship.HelmBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Draws the Helm from {@code geo/block/helm.geo.json}, turned to the chair's facing (GeoBlockRenderer reads the FACING property). */
public class HelmRenderer extends GeoBlockRenderer<HelmBlockEntity> {
    public HelmRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedBlockGeoModel<>(VoidReclaimer.id("helm")));
    }
}
