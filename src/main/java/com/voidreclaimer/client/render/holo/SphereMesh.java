package com.voidreclaimer.client.render.holo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A unit sphere cut into latitude and longitude cells (pure). Row 0 is at the top, column 0 at longitude 0. */
public final class SphereMesh {
    /** One cell: four corners x, y, z in turn, wound the same way round each. */
    public record Cell(int row, int col, float[] corners) {}

    private static final Map<Long, List<Cell>> MESHES = new ConcurrentHashMap<>();

    private SphereMesh() {}

    public static List<Cell> of(int rows, int cols) {
        return MESHES.computeIfAbsent((long) rows << 32 | cols, key -> build(rows, cols));
    }

    private static List<Cell> build(int rows, int cols) {
        List<Cell> cells = new ArrayList<>(rows * cols);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float[] corners = new float[12];
                put(corners, 0, row, col, rows, cols);
                put(corners, 1, row + 1, col, rows, cols);
                put(corners, 2, row + 1, col + 1, rows, cols);
                put(corners, 3, row, col + 1, rows, cols);
                cells.add(new Cell(row, col, corners));
            }
        }
        return List.copyOf(cells);
    }

    private static void put(float[] corners, int corner, int row, int col, int rows, int cols) {
        double polar = Math.PI * row / rows;
        double around = 2 * Math.PI * col / cols;
        corners[corner * 3] = (float) (Math.sin(polar) * Math.cos(around));
        corners[corner * 3 + 1] = (float) Math.cos(polar);
        corners[corner * 3 + 2] = (float) (Math.sin(polar) * Math.sin(around));
    }
}
