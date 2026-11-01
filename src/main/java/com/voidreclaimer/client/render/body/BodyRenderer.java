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
import com.voidreclaimer.client.render.holo.HoloStyle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * Draws anything that wears a body: the chassis rig, with every installed section and module plugged into its bones
 * ({@link BodyPartsLayer}). Called for players from {@link BodyRenderEvents}, which cancels the vanilla player model, and for
 * empty bodies from {@link BodyHostRenderer}.
 */
public final class BodyRenderer extends GeoReplacedEntityRenderer<LivingEntity, BodyAnimatable> {
    /** A fully spread section stands this share of the way further from its anchor. */
    private static final float EXPLODE_SPREAD = 0.9f;
    private static BodyRenderer instance;
    @Nullable
    private static Frame frame;
    /** Whether a part is being drawn into the body ({@link BodyPartsLayer}) and not the body itself. */
    private boolean reRendering;
    /** How far sections are spread for the next draws, 0 to 1; only the assembly view sets it, round its own draw. */
    private static float explode;
    private static boolean showWear;
    private static boolean mirrored;
    @Nullable
    private static BodyView previewView;
    @Nullable
    private static String ghostSocket;
    @Nullable
    private static BodyData.Mount ghostMount;
    private final Map<String, org.joml.Vector3f> spread = new HashMap<>();
    private static float drawnBodyScale = 1f;
    /** While true every draw is a hologram: see {@link #drawHologram}. */
    private static boolean hologram;
    private static boolean hologramWash;
    private static int hologramColor = -1;
    private static float drawnPartScale = 1f;

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

    /**
     * Draws the local player as this body until it is set back to null, with the named section or module as a ghost: the assembly
     * view's preview of a part that is not installed. Nothing on the player changes.
     */
    public static void setPreview(@Nullable BodyView view, @Nullable String socket, @Nullable BodyData.Mount mount) {
        previewView = view;
        ghostSocket = socket;
        ghostMount = mount;
    }

    /** Tints worn sections for the draws until this is set back to false: the assembly view's look at what needs repair. */
    /** Set while a mirrored part is drawn: see {@link #createVerticesOfQuad}. */
    static void setMirrored(boolean value) {
        mirrored = value;
    }

    public static void setShowWear(boolean show) {
        showWear = show;
    }

    static boolean showWear() {
        return showWear;
    }

    static boolean isGhost(String socket) {
        return hologram || previewView != null && ghostMount == null && socket.equals(ghostSocket);
    }

    static boolean isGhost(BodyData.Mount mount) {
        return hologram || previewView != null && mount.equals(ghostMount);
    }

    /**
     * Draws an entity's body as a hologram of about {@code size} blocks, centred on the pose: see-through, upright, whatever
     * way the entity faces. Used by the Holo Projector.
     */
    public static void drawHologram(LivingEntity entity, float size, float alpha, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        float fit = size / (2f * BodyBounds.radius(Bodies.view(entity)) * entity.getScale());
        pose.pushPose();
        pose.scale(fit, fit, fit);
        pose.translate(0f, -entity.getBbHeight() / 2f, 0f);
        hologram = true;
        try {
            hologramColor = HoloStyle.argb(HoloStyle.MODEL_TINT, alpha);
            get().render(entity, 0f, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
            hologramColor = HoloStyle.argb(HoloStyle.CYAN, HoloStyle.WASH_ALPHA * alpha);
            hologramWash = true;
            get().render(entity, 0f, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
        } finally {
            hologram = false;
            hologramWash = false;
            pose.popPose();
        }
    }

    @Override
    public RenderType getRenderType(BodyAnimatable animatable, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
        return hologram ? hologramType(texture) : super.getRenderType(animatable, texture, buffers, partialTick);
    }

    @Override
    public Color getRenderColor(BodyAnimatable animatable, float partialTick, int packedLight) {
        return hologram ? new Color(hologramColor) : super.getRenderColor(animatable, partialTick, packedLight);
    }

    /** The see-through look of a part in a preview or a hologram. */
    static RenderType ghostType(ResourceLocation texture) {
        return hologram ? hologramType(texture) : RenderType.entityTranslucent(texture);
    }

    /** How a ghost part is coloured when {@code preview} is what an assembly preview would use. */
    static int ghostColor(int preview) {
        return hologram ? hologramColor : preview;
    }

    private static RenderType hologramType(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(hologramWash ? HoloStyle.PANEL : texture, false);
    }

    /** Spreads the body's sections apart for the draws until this is set back to 0. */
    public static void setExplode(float amount) {
        explode = amount;
    }

    /** Whether the body is spread: it then holds still. */
    static boolean exploding() {
        return explode > 0f;
    }

    /**
     * Finds each socket's bone and how far it moves. A bone sits inside its parent's, so it moves only by the step from the nearest
     * socket bone above it; top-level ones move from the middle of all the sockets.
     */
    private void measureSpread(BakedGeoModel model, BodyView view, @Nullable BodyRigSpec spec) {
        spread.clear();
        Map<String, GeoBone> bones = new HashMap<>();
        if (spec != null) {
            spec.sockets().values().stream().filter(list -> !list.isEmpty()).map(list -> list.get(0))
                    .forEach(name -> model.getBone(name).ifPresent(bone -> bones.put(name, bone)));
        } else {
            view.chassis().sockets().stream().filter(socket -> !socket.id().equals(view.chassis().coreSocket()))
                    .forEach(socket -> model.getBone(BodyLook.socketBone(socket.id())).ifPresent(bone -> bones.put(bone.getName(), bone)));
        }
        if (bones.isEmpty()) {
            return;
        }
        float[] middle = new float[3];
        for (GeoBone bone : bones.values()) {
            middle[0] += bone.getPivotX() / bones.size();
            middle[1] += bone.getPivotY() / bones.size();
            middle[2] += bone.getPivotZ() / bones.size();
        }
        for (GeoBone bone : bones.values()) {
            float[] anchor = middle;
            for (GeoBone up = bone.getParent(); up != null; up = up.getParent()) {
                if (bones.containsKey(up.getName())) {
                    anchor = new float[] {up.getPivotX(), up.getPivotY(), up.getPivotZ()};
                    break;
                }
            }
            float[] step = com.voidreclaimer.body.assembly.AssemblyCamera.spread(
                    new float[] {bone.getPivotX(), bone.getPivotY(), bone.getPivotZ()}, anchor, EXPLODE_SPREAD);
            spread.put(bone.getName(), new org.joml.Vector3f(step[0] / 16f, step[1] / 16f, step[2] / 16f));
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BodyAnimatable animatable, GeoBone bone, net.minecraft.client.renderer.RenderType renderType,
                                  MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        org.joml.Vector3f step = !isReRender && explode > 0f ? spread.get(bone.getName()) : null;
        if (step == null) {
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
            return;
        }
        poseStack.pushPose();
        poseStack.translate(step.x * explode, step.y * explode, step.z * explode);
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        poseStack.popPose();
    }

    /** The scale the last body was really drawn at, from its render matrix: photo mode's debug layer shows it beside the body's own scale. */
    public static float drawnBodyScale() {
        return drawnBodyScale;
    }

    /** The same for the last part drawn into a body. */
    public static float drawnPartScale() {
        return drawnPartScale;
    }

    /** Rig of the body being drawn. */
    public static String currentRig() {
        return frame == null ? BodyLook.DEFAULT_RIG : frame.rig();
    }

    @Override
    public void render(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BodyView view = previewView != null && entity == Minecraft.getInstance().player ? previewView : Bodies.view(entity);
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
                : BodyTextures.INSTANCE.painted(rig, entity.registryAccess(), cosmetics.casing(view.data().coreSocket()));
        frame = new Frame(view, rig, texture, rigMountBones, inRig, spec, specMounts);
        if (explode > 0f) {
            measureSpread(rigModel, view, spec);
        }
        try {
            if (spec != null) {
                hideEmptySockets(rigModel, view, spec);
            } else {
                hideFilledCaps(rigModel, view);
            }
            boolean probed = entity == Minecraft.getInstance().player && !hologram;
            if (probed) {
                BoneProbe.begin();
            }
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
                    BodyLight.atLeast(packedLight, com.voidreclaimer.ClientConfig.BODY_MIN_LIGHT.get()));
            if (probed) {
                BoneProbe.end();
            }
        } finally {
            frame = null;
        }
    }

    /** GeckoLib labels every living entity with its type: a body lying empty shows no name. */
    @Override
    public boolean shouldShowName(LivingEntity entity) {
        return !(entity instanceof com.voidreclaimer.link.BodyHost) && super.shouldShowName(entity);
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
        if (reRendering) {
            // GeoReplacedEntityRenderer applies the entity's scale and turn on every pass, re-renders included, but a part drawn at a
            // bone is already in the body's space: undo the scale and skip the turn, or parts come out scale cubed and spinning.
            poseStack.scale(1f / nativeScale, 1f / nativeScale, 1f / nativeScale);
            if (!hologram) {
                drawnPartScale = matrixScale(poseStack);
            }
            return;
        }
        LivingEntity entity = getCurrentEntity();
        if (hologram) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f + (frame == null || frame.spec() == null ? 0f : frame.spec().yaw())));
            return;
        }
        drawnBodyScale = matrixScale(poseStack);
        Frame current = frame;
        String rig = current == null ? BodyLook.DEFAULT_RIG : current.rig();
        BodyRigSpec spec = current == null ? null : current.spec();
        boolean pitches = spec != null ? spec.pitches() : BodyLook.pitches(rig);
        float yaw = pitches ? entity.getViewYRot(partialTick) : Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(BodyLook.turn(yaw, pitches ? entity.getViewXRot(partialTick) : 0f, spec == null ? 0f : spec.yaw()));
        BodyRigSpec.Lean lean = spec == null ? BodyRigSpec.Lean.NONE : spec.lean();
        if (!lean.isNone()) {
            applyLean(poseStack, entity, lean, pitches ? entity.getViewYRot(partialTick) : Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot), partialTick, nativeScale);
        }
    }

    /** A mirror turns every face inside out, and a pass that culls, like the accent's, would drop them all: the corners go the other way round. */
    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        if (!mirrored) {
            super.createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, colour);
            return;
        }
        GeoVertex[] vertices = quad.vertices();
        for (int i = vertices.length - 1; i >= 0; i--) {
            GeoVertex vertex = vertices[i];
            Vector3f position = vertex.position();
            Vector4f placed = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1f));
            buffer.addVertex(placed.x(), placed.y(), placed.z(), colour, vertex.texU(), vertex.texV(), packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }

    private static float matrixScale(PoseStack poseStack) {
        return poseStack.last().pose().getScale(new org.joml.Vector3f()).y();
    }

    private static final BodyLean.State LEAN = new BodyLean.State();

    /** Leans the model into its motion about the middle of the body (the rig's front is local +Z after the turn above). */
    private static void applyLean(PoseStack poseStack, LivingEntity entity, BodyRigSpec.Lean lean, float yaw, float partialTick, float scale) {
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        LEAN.update(entity.getId(), entity.tickCount, BodyLook.forward(dx, dz, yaw), BodyLook.lateral(dx, dz, yaw), lean);
        BodyLean.Angles angles = LEAN.at(entity.getId(), partialTick);
        float pivot = entity.getBbHeight() * 0.5f / scale;
        poseStack.translate(0.0, pivot, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(angles.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angles.roll()));
        poseStack.translate(0.0, -pivot, 0.0);
    }

    /** The way a body's model faces, in degrees of yaw: the view for bodies that pitch with it, the body's own turn for an upright one. */
    public static float facing(LivingEntity entity) {
        BodyView view = Bodies.view(entity);
        SectionStats core = Parts.section(view.data().core());
        String rig = BodyLook.rig(core == null ? null : core.chassis().orElse(null));
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        return (spec != null ? spec.pitches() : BodyLook.pitches(rig)) ? entity.getYRot() : entity.yBodyRot;
    }

    /** Where the rig's origin sits above the player's feet: its middle for flyers, the pelvis height for the upright Mech. */
    public static double originHeight(String rig, LivingEntity entity) {
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        if (spec != null) {
            return (spec.grounded() ? 0.0 : entity.getBbHeight() / 2.0) + spec.offsetY() / 16.0 * entity.getScale();
        }
        return "mech".equals(rig) ? 15.0 / 16.0 * entity.getScale() : entity.getBbHeight() / 2.0;
    }

    @Override
    public void preRender(PoseStack poseStack, BodyAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        reRendering = isReRender;
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
