package com.voidreclaimer.client.render.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Parts;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.api.body.SectionStats;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

/** How far a body's model reaches from its origin, so a view can fit any body from the Drone to the Titan. */
public final class BodyBounds {
    /** Room for sections a composed rig plugs in at its bones: their models are not part of the rig. */
    private static final float SECTION_ROOM = 1f;
    private static final float DEFAULT_RADIUS = 1f;
    /** Read every frame by the camera, so measured once per baked model; a resource reload makes new models. */
    private static final java.util.Map<BakedGeoModel, Float> RADII = new java.util.WeakHashMap<>();
    private static final java.util.Map<BakedGeoModel, float[]> BOXES = new java.util.WeakHashMap<>();

    private BodyBounds() {}

    /** Half the diagonal of the box round the rig this body uses, in blocks at scale 1: how far it reaches from its middle. */
    public static float radius(BodyView view) {
        SectionStats core = Parts.section(view.data().core());
        String rig = BodyLook.rig(core == null ? null : core.chassis().orElse(null));
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(spec != null ? spec.model() : VoidReclaimer.id("geo/body/" + rig + ".geo.json"));
        if (model == null) {
            return DEFAULT_RADIUS;
        }
        return RADII.computeIfAbsent(model, baked -> measure(baked, spec == null));
    }

    /** The longest side of the box round this body's rig, in blocks at scale 1; 0 when the model is not loaded. */
    public static float length(BodyView view) {
        SectionStats core = Parts.section(view.data().core());
        String rig = BodyLook.rig(core == null ? null : core.chassis().orElse(null));
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(spec != null ? spec.model() : VoidReclaimer.id("geo/body/" + rig + ".geo.json"));
        if (model == null) {
            return 0f;
        }
        float[] box = box(model, spec == null);
        return Math.max(0f, Math.max(box[3] - box[0], Math.max(box[4] - box[1], box[5] - box[2])));
    }

    /** The box round a whole model, in blocks: min x, y, z, then max x, y, z. */
    public static float[] box(BakedGeoModel model) {
        return BOXES.computeIfAbsent(model, baked -> box(baked, false));
    }

    private static float[] box(BakedGeoModel model, boolean composed) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (GeoBone bone : model.topLevelBones()) {
            reach(bone, composed, box);
        }
        return box;
    }

    private static float measure(BakedGeoModel model, boolean composed) {
        float[] box = box(model, composed);
        float x = box[3] - box[0];
        float y = box[4] - box[1];
        float z = box[5] - box[2];
        return x < 0f ? DEFAULT_RADIUS : (float) Math.sqrt(x * x + y * y + z * z) / 2f;
    }

    private static void grow(float[] box, float x, float y, float z) {
        box[0] = Math.min(box[0], x);
        box[1] = Math.min(box[1], y);
        box[2] = Math.min(box[2], z);
        box[3] = Math.max(box[3], x);
        box[4] = Math.max(box[4], y);
        box[5] = Math.max(box[5], z);
    }

    private static void reach(GeoBone bone, boolean composed, float[] box) {
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                for (GeoVertex vertex : quad.vertices()) {
                    grow(box, vertex.position().x, vertex.position().y, vertex.position().z);
                }
            }
        }
        if (composed) {
            float x = bone.getPivotX() / 16f;
            float y = bone.getPivotY() / 16f;
            float z = bone.getPivotZ() / 16f;
            grow(box, x - SECTION_ROOM, y - SECTION_ROOM, z - SECTION_ROOM);
            grow(box, x + SECTION_ROOM, y + SECTION_ROOM, z + SECTION_ROOM);
        }
        for (GeoBone child : bone.getChildBones()) {
            reach(child, composed, box);
        }
    }
}
