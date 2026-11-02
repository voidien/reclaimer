package com.voidreclaimer.client.render.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.Parts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

/** How far a body's model reaches from its origin, so a view can fit any body from the Drone to the Titan, and how long it is. */
public final class BodyBounds {
    /** Room for a section whose model is missing. */
    private static final float SECTION_ROOM = 1f;
    private static final float DEFAULT_RADIUS = 1f;
    /** Exhaust and other effects: they come and go, and aren't the body. */
    private static final String EFFECT = "fx_";
    private static final String CAP = "cap_";
    private static final float PIXELS = 16f;
    /** Read every frame by the camera, so measured once per baked model; a resource reload makes new models. */
    private static final java.util.Map<BakedGeoModel, float[]> BOXES = new java.util.WeakHashMap<>();
    /** A composed body's size depends on what is fitted to it as well. */
    private static final java.util.Map<BakedGeoModel, java.util.Map<BodyData, float[]>> COMPOSED = new java.util.WeakHashMap<>();

    private BodyBounds() {}

    /** Half the diagonal of the box round this body, in blocks at scale 1: how far it reaches from its middle. */
    public static float radius(BodyView view) {
        float[] box = box(view);
        float x = box[3] - box[0];
        float y = box[4] - box[1];
        float z = box[5] - box[2];
        return x < 0f ? DEFAULT_RADIUS : (float) Math.sqrt(x * x + y * y + z * z) / 2f;
    }

    /** The longest side of the box round this body, in blocks at scale 1; 0 when its model is not loaded. */
    public static float length(BodyView view) {
        float[] box = box(view);
        return Math.max(0f, Math.max(box[3] - box[0], Math.max(box[4] - box[1], box[5] - box[2])));
    }

    /** The box round a body as it is drawn: a whole model, or a composed rig with each fitted section's own model at its bone. */
    public static float[] box(BodyView view) {
        SectionStats core = Parts.section(view.data().core());
        String rig = BodyLook.rig(view.chassis(), core);
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(spec != null ? spec.model() : VoidReclaimer.id("geo/body/" + rig + ".geo.json"));
        if (model == null) {
            return empty();
        }
        if (spec != null) {
            return box(model);
        }
        return COMPOSED.computeIfAbsent(model, baked -> new java.util.WeakHashMap<>()).computeIfAbsent(view.data(), body -> composed(model, view));
    }

    /** The box round a whole model, in blocks: min x, y, z, then max x, y, z. */
    public static float[] box(BakedGeoModel model) {
        return BOXES.computeIfAbsent(model, baked -> {
            float[] box = empty();
            for (GeoBone bone : baked.topLevelBones()) {
                reach(bone, new Matrix4f(), box);
            }
            return box;
        });
    }

    private static float[] empty() {
        return new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
    }

    private static float[] composed(BakedGeoModel rig, BodyView view) {
        float[] box = empty();
        for (GeoBone bone : rig.topLevelBones()) {
            composed(bone, view, box);
        }
        return box;
    }

    /** One bone of a composed rig, as {@code BodyPartsLayer} draws it: a fitted socket's section sits at the bone, and its cap is hidden. */
    private static void composed(GeoBone bone, BodyView view, float[] box) {
        String name = bone.getName();
        boolean capHidden = name.startsWith(CAP) && !view.data().section(name.substring(CAP.length())).isEmpty();
        if (!capHidden) {
            cubes(bone, new Matrix4f(), box);
        }
        ItemStack section = view.data().section(name);
        if (!section.isEmpty() && !name.equals(view.data().coreSocket()) && view.chassis().socket(name) != null) {
            section(bone, section, view.chassis().socket(name).mirror(), box);
        }
        for (GeoBone child : bone.getChildBones()) {
            composed(child, view, box);
        }
    }

    private static void section(GeoBone bone, ItemStack section, boolean mirrored, float[] box) {
        float x = bone.getPivotX() / PIXELS;
        float y = bone.getPivotY() / PIXELS;
        float z = bone.getPivotZ() / PIXELS;
        String path = BuiltInRegistries.ITEM.getKey(section.getItem()).getPath();
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(VoidReclaimer.id("geo/section/" + path + ".geo.json"));
        if (model == null) {
            grow(box, new Vector3f(x - SECTION_ROOM, y - SECTION_ROOM, z - SECTION_ROOM));
            grow(box, new Vector3f(x + SECTION_ROOM, y + SECTION_ROOM, z + SECTION_ROOM));
            return;
        }
        Matrix4f at = new Matrix4f().translate(x, y, z).scale(mirrored ? -1f : 1f, 1f, 1f);
        for (GeoBone top : model.topLevelBones()) {
            reach(top, at, box);
        }
    }

    private static void reach(GeoBone bone, Matrix4f at, float[] box) {
        if (bone.getName().startsWith(EFFECT)) {
            return;
        }
        cubes(bone, at, box);
        for (GeoBone child : bone.getChildBones()) {
            reach(child, at, box);
        }
    }

    /** Each cube turns about its own pivot, Z then Y then X, as GeckoLib draws it: a vertex alone says where it was before turning. */
    private static void cubes(GeoBone bone, Matrix4f at, float[] box) {
        for (GeoCube cube : bone.getCubes()) {
            float x = (float) cube.pivot().x / PIXELS;
            float y = (float) cube.pivot().y / PIXELS;
            float z = (float) cube.pivot().z / PIXELS;
            Matrix4f turned = new Matrix4f(at).translate(x, y, z).rotateZ((float) cube.rotation().z).rotateY((float) cube.rotation().y)
                    .rotateX((float) cube.rotation().x).translate(-x, -y, -z);
            for (GeoQuad quad : cube.quads()) {
                for (GeoVertex vertex : quad.vertices()) {
                    grow(box, turned.transformPosition(vertex.position(), new Vector3f()));
                }
            }
        }
    }

    private static void grow(float[] box, Vector3f point) {
        box[0] = Math.min(box[0], point.x);
        box[1] = Math.min(box[1], point.y);
        box[2] = Math.min(box[2], point.z);
        box[3] = Math.max(box[3], point.x);
        box[4] = Math.max(box[4], point.y);
        box[5] = Math.max(box[5], point.z);
    }
}
