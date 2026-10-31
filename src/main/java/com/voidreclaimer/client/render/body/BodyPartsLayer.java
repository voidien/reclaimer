package com.voidreclaimer.client.render.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.Cosmetics;
import java.util.Optional;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Plugs the installed sections and modules into the rig. A section sits at the rig bone named after its socket; a module at
 * the rig's {@code mount_*} bone, or, when the rig has none for it, at the matching mount bone inside its section's model.
 * Every part is drawn twice: the base texture with the casing painted in, and the accent mask tinted with the accent colour
 * at full brightness (the lights, trim and pipes glow).
 */
public final class BodyPartsLayer extends GeoRenderLayer<BodyAnimatable> {
    public BodyPartsLayer(GeoRenderer<BodyAnimatable> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, BodyAnimatable animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        BodyRenderer.Frame frame = BodyRenderer.frame();
        if (frame == null) {
            return;
        }
        if (BoneProbe.recording()) {
            probe(poseStack, bone, frame);
        }
        if (frame.spec() != null) {
            renderSpecMounts(poseStack, animatable, bone, frame, bufferSource, partialTick, packedLight, packedOverlay);
            return;
        }
        BodyView view = frame.view();
        String name = bone.getName();
        ItemStack section = view.data().section(name);
        if (!section.isEmpty() && !name.equals(view.data().coreSocket()) && view.chassis().socket(name) != null) {
            poseStack.pushPose();
            moveToPivot(poseStack, bone);
            if (view.chassis().socket(name).mirror()) {
                poseStack.scale(-1f, 1f, 1f);
            }
            drawPart("section", section, name, frame, poseStack, bufferSource, animatable, partialTick, packedLight, packedOverlay);
            drawInnerModules(name, section, frame, poseStack, bufferSource, animatable, partialTick, packedLight, packedOverlay);
            poseStack.popPose();
        }
        BodyData.Mount mount = frame.rigMountBones().get(name);
        if (mount != null) {
            ItemStack module = view.data().module(mount);
            if (!module.isEmpty()) {
                poseStack.pushPose();
                moveToPivot(poseStack, bone);
                drawPart("module", module, mount.socket(), frame, poseStack, bufferSource, animatable, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        }
    }

    /** Modules on a whole-model rig: at the bone the spec names for their mount, moved by its offset and turned by its yaw. */
    private void renderSpecMounts(PoseStack poseStack, BodyAnimatable animatable, GeoBone bone, BodyRenderer.Frame frame,
                                  MultiBufferSource bufferSource, float partialTick, int light, int overlay) {
        java.util.List<BodyRenderer.PlacedMount> placed = frame.specMounts().get(bone.getName());
        if (placed == null) {
            return;
        }
        for (BodyRenderer.PlacedMount entry : placed) {
            ItemStack module = frame.view().data().module(entry.mount());
            if (module.isEmpty()) {
                continue;
            }
            BodyRigSpec.Mount place = entry.place();
            poseStack.pushPose();
            moveToPivot(poseStack, bone);
            // Offsets use the axes of the pivots in the .geo.json file, where x is mirrored against the rendered model.
            poseStack.translate(-place.x() / 16f, place.y() / 16f, place.z() / 16f);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(place.yaw()));
            drawPart("module", module, entry.mount().socket(), frame, poseStack, bufferSource, animatable, partialTick, light, overlay);
            poseStack.popPose();
        }
    }

    /** Modules whose mount is a bone of the section's own model (the rig has no bone for them). */
    private void drawInnerModules(String socket, ItemStack section, BodyRenderer.Frame frame, PoseStack poseStack, MultiBufferSource bufferSource,
                                  BodyAnimatable animatable, float partialTick, int light, int overlay) {
        BakedGeoModel sectionModel = model("section", section);
        if (sectionModel == null) {
            return;
        }
        for (BodyData.Mount mount : frame.view().mounts(socket)) {
            if (frame.inRigMounts().contains(mount)) {
                continue;
            }
            ItemStack module = frame.view().data().module(mount);
            Optional<GeoBone> mountBone = sectionModel.getBone(BodyLook.sectionMountBone(mount));
            if (BoneProbe.recording() && mountBone.isPresent()) {
                GeoBone inner = mountBone.get();
                addPoint(poseStack, BodyLook.rigMountBone(frame.view().data().coreSocket(), mount), BoneProbe.Kind.MOUNT, !module.isEmpty(),
                        inner.getPivotX() / 16f, inner.getPivotY() / 16f, inner.getPivotZ() / 16f);
            }
            if (module.isEmpty() || mountBone.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            moveToPivot(poseStack, mountBone.get());
            drawPart("module", module, socket, frame, poseStack, bufferSource, animatable, partialTick, light, overlay);
            poseStack.popPose();
        }
    }

    /** Adds this bone's sockets and mounts to the {@link BoneProbe}; at the same spots the parts are drawn. */
    private static void probe(PoseStack poseStack, GeoBone bone, BodyRenderer.Frame frame) {
        BodyView view = frame.view();
        String name = bone.getName();
        float x = bone.getPivotX() / 16f;
        float y = bone.getPivotY() / 16f;
        float z = bone.getPivotZ() / 16f;
        if (frame.spec() == null) {
            if (view.chassis().socket(name) != null) {
                addPoint(poseStack, name, BoneProbe.Kind.SOCKET, !view.data().section(name).isEmpty(), x, y, z);
            }
            BodyData.Mount mount = frame.rigMountBones().get(name);
            if (mount != null) {
                addPoint(poseStack, name, BoneProbe.Kind.MOUNT, !view.data().module(mount).isEmpty(), x, y, z);
            }
            return;
        }
        frame.spec().sockets().forEach((socket, bones) -> {
            if (!bones.isEmpty() && bones.get(0).equals(name)) {
                addPoint(poseStack, socket, BoneProbe.Kind.SOCKET, !view.data().section(socket).isEmpty(), x, y, z);
            }
        });
        for (BodyData.Mount mount : view.mounts()) {
            String mountName = BodyLook.rigMountBone(view.data().coreSocket(), mount);
            BodyRigSpec.Mount place = frame.spec().mounts().get(mountName);
            if (place != null && place.bone().equals(name)) {
                addPoint(poseStack, mountName, BoneProbe.Kind.MOUNT, !view.data().module(mount).isEmpty(),
                        x - place.x() / 16f, y + place.y() / 16f, z + place.z() / 16f);
            }
        }
    }

    private static void addPoint(PoseStack poseStack, String name, BoneProbe.Kind kind, boolean filled, float x, float y, float z) {
        BoneProbe.add(name, kind, filled, poseStack.last().pose().transformPosition(x, y, z, new org.joml.Vector3f()));
    }

    private static void moveToPivot(PoseStack poseStack, GeoBone bone) {
        poseStack.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
    }

    private static String path(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static BakedGeoModel model(String folder, ItemStack stack) {
        return GeckoLibCache.getBakedModels().get(VoidReclaimer.id("geo/" + folder + "/" + path(stack) + ".geo.json"));
    }

    /** Draws one part's model with the paint of the section in {@code paintSocket}; nothing when the art has no model for the item. */
    private void drawPart(String folder, ItemStack stack, String paintSocket, BodyRenderer.Frame frame, PoseStack poseStack,
                          MultiBufferSource bufferSource, BodyAnimatable animatable, float partialTick, int light, int overlay) {
        BakedGeoModel model = model(folder, stack);
        if (model == null) {
            return;
        }
        String part = path(stack);
        Cosmetics cosmetics = frame.cosmetics();
        var access = net.minecraft.client.Minecraft.getInstance().level.registryAccess();
        ResourceLocation texture = BodyTextures.INSTANCE.painted(part, access, cosmetics.casing(paintSocket));
        RenderType base = RenderType.entityCutoutNoCull(texture);
        renderer.reRender(model, poseStack, bufferSource, animatable, base, bufferSource.getBuffer(base), partialTick, light, overlay, 0xFFFFFFFF);
        RenderType accent = RenderType.entityTranslucent(BodyTextures.accent(part));
        renderer.reRender(model, poseStack, bufferSource, animatable, accent, bufferSource.getBuffer(accent), partialTick,
                LightTexture.FULL_BRIGHT, overlay, 0xFF000000 | cosmetics.accent(paintSocket));
    }
}
