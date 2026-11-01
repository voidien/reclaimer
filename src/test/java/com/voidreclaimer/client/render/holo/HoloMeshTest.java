package com.voidreclaimer.client.render.holo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

class HoloMeshTest {
    private static void quad(HoloMesh mesh, float x) {
        for (int i = 0; i < 4; i++) {
            mesh.addVertex(x + i, i, -i).setUv(0.5f, 0.25f);
        }
    }

    @Test
    void itRecordsVerticesAndTheirBox() {
        HoloMesh mesh = new HoloMesh();
        quad(mesh, 10f);
        assertEquals(4, mesh.vertices());
        assertArrayEquals(new float[] {10f, 0f, -3f, 13f, 3f, 0f}, mesh.box());
    }

    @Test
    void aReplayGivesTheSameVerticesThroughThePose() {
        HoloMesh mesh = new HoloMesh();
        quad(mesh, 0f);
        PoseStack pose = new PoseStack();
        pose.translate(100f, 0f, 0f);
        HoloMesh out = new HoloMesh();
        mesh.replay(pose.last(), out, 0xFF112233);
        assertEquals(4, out.vertices());
        assertEquals(100f, out.box()[0]);
        assertEquals(103f, out.box()[3]);
    }

    @Test
    void aFullMeshStopsAtAWholeQuad() {
        HoloMesh mesh = new HoloMesh();
        for (int i = 0; i < HoloMesh.MAX_VERTICES / 4 + 50; i++) {
            quad(mesh, i);
        }
        assertEquals(HoloMesh.MAX_VERTICES, mesh.vertices());
        assertEquals(0, mesh.vertices() % 4);
    }
}
