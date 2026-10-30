package com.voidreclaimer.client.render.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.body.Parts;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Draws a player as their body: the chassis rig, with every installed section and module plugged into its bones
 * ({@link BodyPartsLayer}). Called from {@link BodyRenderEvents}, which cancels the vanilla player model.
 */
public final class BodyRenderer extends GeoReplacedEntityRenderer<Player, BodyAnimatable> {
    private static BodyRenderer instance;
    @Nullable
    private static Frame frame;

    /**
     * What is being drawn this frame.
     *
     * @param view         the body with its layout
     * @param rig          the art rig ({@code drone}, {@code mech} or {@code ship})
     * @param rigTexture   the rig's texture with the body casing painted in
     * @param rigMountBones module mounts that have a bone in the rig, by bone name
     * @param inRigMounts  the mounts that are drawn on rig bones (the others sit inside their section's model)
     * @param spec         the rig's whole-model spec, or null for a composed rig
     * @param specMounts   for a whole-model rig: the installed modules to draw, by the bone they follow
     */
    public record Frame(BodyView view, String rig, ResourceLocation rigTexture, Map<String, BodyData.Mount> rigMountBones,
                        Set<BodyData.Mount> inRigMounts, @Nullable BodyRigSpec spec, Map<String, List<PlacedMount>> specMounts) {
        public ResourceLocation modelResource() {
            return spec != null ? spec.model() : com.voidreclaimer.VoidReclaimer.id("geo/body/" + rig + ".geo.json");
        }

        public ResourceLocation animationResource() {
            return spec != null ? spec.animations() : com.voidreclaimer.VoidReclaimer.id("animations/body/" + rig + ".animation.json");
        }

        public Cosmetics cosmetics() {
            return view.data().cosmetics();
        }
    }

    /** A module to draw on a whole-model rig: which mount it is on and where the spec puts it. */
    public record PlacedMount(BodyData.Mount mount, BodyRigSpec.Mount place) {}

    private BodyRenderer(EntityRendererProvider.Context context) {
        super(context, new BodyModel(), BodyAnimatable.INSTANCE);
        addRenderLayer(new BodyPartsLayer(this));
        shadowRadius = 0f;
    }

    /** The shared renderer, built on first use (the renderer context needs the game's loaded resources). */
    public static BodyRenderer get() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new BodyRenderer(new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(),
                    mc.getBlockRenderer(), mc.gameRenderer.itemInHandRenderer, mc.getResourceManager(), mc.getEntityModels(), mc.font));
        }
        return instance;
    }

    @Nullable
    public static Frame frame() {
        return frame;
    }

    /** Rig of the body being drawn. */
    public static String currentRig() {
        return frame == null ? BodyLook.DEFAULT_RIG : frame.rig();
    }

    @Override
    public void render(Player player, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BodyView view = Bodies.view(player);
        SectionStats core = Parts.section(view.data().core());
        String rig = BodyLook.rig(core == null ? null : core.chassis().orElse(null));
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        ResourceLocation modelId = spec != null ? spec.model() : com.voidreclaimer.VoidReclaimer.id("geo/body/" + rig + ".geo.json");
        BakedGeoModel rigModel = GeckoLibCache.getBakedModels().get(modelId);
        if (rigModel == null) {
            return;
        }
        Cosmetics cosmetics = view.data().cosmetics();
        Map<String, BodyData.Mount> rigMountBones = new HashMap<>();
        Set<BodyData.Mount> inRig = new HashSet<>();
        for (BodyData.Mount mount : spec != null ? List.<BodyData.Mount>of() : view.mounts()) {
            String boneName = BodyLook.rigMountBone(view.data().coreSocket(), mount);
            if (rigModel.getBone(boneName).isPresent()) {
                rigMountBones.put(boneName, mount);
                inRig.add(mount);
            }
        }
        Map<String, List<PlacedMount>> specMounts = new HashMap<>();
        if (spec != null) {
            for (BodyData.Mount mount : view.mounts()) {
                BodyRigSpec.Mount place = spec.mounts().get(BodyLook.rigMountBone(view.data().coreSocket(), mount));
                if (place != null && !view.data().module(mount).isEmpty() && rigModel.getBone(place.bone()).isPresent()) {
                    specMounts.computeIfAbsent(place.bone(), bone -> new java.util.ArrayList<>()).add(new PlacedMount(mount, place));
                }
            }
        }
        ResourceLocation texture = spec != null ? spec.texture()
                : BodyTextures.INSTANCE.painted(rig, player.registryAccess(), cosmetics.casing(view.data().coreSocket()));
        frame = new Frame(view, rig, texture, rigMountBones, inRig, spec, specMounts);
        try {
            if (spec != null) {
                hideEmptySockets(rigModel, view, spec);
            } else {
                hideFilledCaps(rigModel, view);
            }
            withScale(player.getScale());
            super.render(player, entityYaw, partialTick, poseStack, bufferSource,
                    BodyLight.atLeast(packedLight, com.voidreclaimer.ClientConfig.BODY_MIN_LIGHT.get()));
        } finally {
            frame = null;
        }
    }

    /** A whole-model rig shows a limb only when the section of its socket is installed; the bone's children go with it. */
    private static void hideEmptySockets(BakedGeoModel rigModel, BodyView view, BodyRigSpec spec) {
        spec.sockets().forEach((socket, bones) -> {
            boolean empty = view.data().section(socket).isEmpty();
            for (String name : bones) {
                rigModel.getBone(name).ifPresent(bone -> {
                    bone.setHidden(empty);
                    bone.setChildrenHidden(empty);
                });
            }
        });
        spec.socketParts().forEach((socket, bones) -> {
            boolean empty = view.data().section(socket).isEmpty();
            for (String name : bones) {
                rigModel.getBone(name).ifPresent(bone -> bone.setHidden(empty));
            }
        });
    }

    /** Cap bones plug sockets that have no section; hide the ones whose socket is filled. */
    private static void hideFilledCaps(BakedGeoModel rigModel, BodyView view) {
        Chassis chassis = view.chassis();
        for (Socket socket : chassis.sockets()) {
            boolean filled = !view.data().section(socket.id()).isEmpty();
            rigModel.getBone("cap_" + socket.id()).ifPresent(bone -> bone.setHidden(filled));
            rigModel.getBone(BodyLook.socketBone(socket.id())).ifPresent(bone -> {
                for (GeoBone child : bone.getChildBones()) {
                    if (BodyLook.isCap(child.getName())) {
                        child.setHidden(filled);
                    }
                }
            });
        }
    }

    /** Fly facing where the player looks (nose up and down for drones and ships); the upright Mech only turns. */
    @Override
    protected void applyRotations(BodyAnimatable animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        Player player = getCurrentEntity();
        Frame current = frame;
        String rig = current == null ? BodyLook.DEFAULT_RIG : current.rig();
        BodyRigSpec spec = current == null ? null : current.spec();
        boolean pitches = spec != null ? spec.pitches() : BodyLook.pitches(rig);
        float yaw = pitches ? player.getViewYRot(partialTick) : Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw + (spec == null ? 0f : spec.yaw())));
        if (pitches) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-player.getViewXRot(partialTick)));
        }
        BodyRigSpec.Lean lean = spec == null ? BodyRigSpec.Lean.NONE : spec.lean();
        if (!lean.isNone()) {
            applyLean(poseStack, player, lean, pitches ? player.getViewYRot(partialTick) : Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot), partialTick);
        }
    }

    private static final BodyLean.State LEAN = new BodyLean.State();

    /** Leans the model into its motion about the middle of the body (the rig's front is local +Z after the turn above). */
    private static void applyLean(PoseStack poseStack, Player player, BodyRigSpec.Lean lean, float yaw, float partialTick) {
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        LEAN.update(player.getId(), player.tickCount, BodyLook.forward(dx, dz, yaw), BodyLook.lateral(dx, dz, yaw), lean);
        BodyLean.Angles angles = LEAN.at(player.getId(), partialTick);
        float pivot = player.getBbHeight() * 0.5f;
        poseStack.translate(0.0, pivot, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(angles.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angles.roll()));
        poseStack.translate(0.0, -pivot, 0.0);
    }

    /** Where the rig's origin sits above the player's feet: its middle for flyers, the pelvis height for the upright Mech. */
    public static double originHeight(String rig, Player player) {
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        if (spec != null) {
            return (spec.grounded() ? 0.0 : player.getBbHeight() / 2.0) + spec.offsetY() / 16.0 * player.getScale();
        }
        return "mech".equals(rig) ? 15.0 / 16.0 * player.getScale() : player.getBbHeight() / 2.0;
    }

    @Override
    public void preRender(PoseStack poseStack, BodyAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        if (!isReRender) {
            poseStack.translate(0.0, originHeight(currentRig(), getCurrentEntity()), 0.0);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /** Sections currently installed, for tests and layers. */
    static List<String> filledSockets(BodyView view) {
        return view.chassis().sockets().stream().map(Socket::id).filter(id -> !view.data().section(id).isEmpty()).toList();
    }
}
