package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.BodyData;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ThrusterExhaustTest {
    private static BoneProbe.Point mount(ModuleKind kind, boolean filled, float x) {
        return new BoneProbe.Point("m", BoneProbe.Kind.MOUNT, filled, new Vector3f(x, 1f, 2f), "back", new BodyData.Mount("back", kind, 0));
    }

    @Test
    void itComesFromInstalledThrustersOnly() {
        List<Vec3> origins = ThrusterExhaust.origins(List.of(mount(ModuleKind.THRUSTER, true, 3f), mount(ModuleKind.THRUSTER, false, 4f), mount(ModuleKind.SENSOR, true, 5f)), new Vec3(10, 20, 30));
        assertEquals(List.of(new Vec3(13, 21, 32)), origins, "one installed thruster, placed in the world from the camera");
    }

    @Test
    void aBodyWithNoThrusterMakesNoExhaust() {
        assertTrue(ThrusterExhaust.installed(List.of(new BodyData.Mount("back", ModuleKind.SENSOR, 0), new BodyData.Mount("back", ModuleKind.THRUSTER, 1))));
        assertFalse(ThrusterExhaust.installed(List.of(new BodyData.Mount("back", ModuleKind.SENSOR, 0))));
    }

    @Test
    void anEmptyListMeansTheCallerFallsBack() {
        assertTrue(ThrusterExhaust.origins(List.of(), Vec3.ZERO).isEmpty());
    }
}
