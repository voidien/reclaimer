package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

class BodyBoundsTest {
    /** A cube two blocks wide round the origin, turned about {@code pivot} (in pixels) by {@code rotation} (in radians). */
    private static GeoCube cube(Vec3 pivot, Vec3 rotation) {
        GeoVertex[] corners = new GeoVertex[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new GeoVertex((i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1);
        }
        return new GeoCube(new GeoQuad[] {new GeoQuad(corners, new Vector3f(0f, 1f, 0f), Direction.UP)}, pivot, rotation, new Vec3(2, 2, 2), 0.0, false);
    }

    private static GeoBone bone(String name, GeoCube cube) {
        GeoBone bone = new GeoBone(null, name, false, null, false, false);
        bone.getCubes().add(cube);
        return bone;
    }

    /** The cache knows a model by its bones, so each test names its own. */
    private static float[] box(GeoBone... bones) {
        return BodyBounds.box(new BakedGeoModel(List.of(bones), null));
    }

    @Test
    void aPlainCubeReachesItsCorners() {
        float[] box = box(bone("plain", cube(Vec3.ZERO, Vec3.ZERO)));
        assertEquals(-1f, box[0], 1e-4f);
        assertEquals(1f, box[3], 1e-4f);
    }

    @Test
    void aTurnedCubeReachesFurtherThanItsCorners() {
        float[] box = box(bone("turned", cube(Vec3.ZERO, new Vec3(0, Math.PI / 4, 0))));
        assertEquals((float) Math.sqrt(2), box[3], 1e-4f, "an eighth of a turn puts a corner on the x axis");
        assertEquals(1f, box[4], 1e-4f, "a turn about y leaves the height alone");
    }

    @Test
    void aCubeTurnsAboutItsOwnPivotInPixels() {
        float[] box = box(bone("pivoted", cube(new Vec3(16, 0, 0), new Vec3(0, Math.PI, 0))));
        assertEquals(1f, box[0], 1e-4f, "half a turn about a pivot one block along lands one block further on");
        assertEquals(3f, box[3], 1e-4f);
    }

    @Test
    void anEffectIsNotPartOfTheBody() {
        float[] box = box(bone("flamed", cube(Vec3.ZERO, Vec3.ZERO)), bone("fx_flame", cube(new Vec3(160, 0, 0), new Vec3(0, Math.PI, 0))));
        assertEquals(1f, box[3], 1e-4f, "the flame counted");
    }
}
