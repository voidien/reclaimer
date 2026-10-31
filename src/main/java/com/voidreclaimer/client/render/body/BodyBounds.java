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

    private BodyBounds() {}

    /** The radius in blocks at scale 1 of the rig this body uses. */
    public static float radius(BodyView view) {
        SectionStats core = Parts.section(view.data().core());
        String rig = BodyLook.rig(core == null ? null : core.chassis().orElse(null));
        BodyRigSpec spec = BodyRigSpec.Loader.get(rig);
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(spec != null ? spec.model() : VoidReclaimer.id("geo/body/" + rig + ".geo.json"));
        if (model == null) {
            return DEFAULT_RADIUS;
        }
        float radius = 0f;
        for (GeoBone bone : model.topLevelBones()) {
            radius = Math.max(radius, reach(bone, spec == null));
        }
        return radius > 0f ? radius : DEFAULT_RADIUS;
    }

    private static float reach(GeoBone bone, boolean composed) {
        float radius = 0f;
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                for (GeoVertex vertex : quad.vertices()) {
                    radius = Math.max(radius, vertex.position().length());
                }
            }
        }
        if (composed) {
            float x = bone.getPivotX() / 16f;
            float y = bone.getPivotY() / 16f;
            float z = bone.getPivotZ() / 16f;
            radius = Math.max(radius, (float) Math.sqrt(x * x + y * y + z * z) + SECTION_ROOM);
        }
        for (GeoBone child : bone.getChildBones()) {
            radius = Math.max(radius, reach(child, composed));
        }
        return radius;
    }
}
