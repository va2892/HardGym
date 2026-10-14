package com.hardgym.client;

import com.hardgym.block.EquipmentRackLayout;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;

/** Immutable exposed plate surfaces, built once instead of allocating cuboids every frame. */
final class RackPlateMesh {
    private static final int[][][] FACES = {
            {{0,0,0},{0,1,0},{1,1,0},{1,0,0}},
            {{0,0,1},{1,0,1},{1,1,1},{0,1,1}},
            {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
            {{1,0,0},{1,1,0},{1,1,1},{1,0,1}},
            {{0,0,0},{1,0,0},{1,0,1},{0,0,1}},
            {{0,1,1},{1,1,1},{1,1,0},{0,1,0}}
    };
    private static final float[][] NORMALS = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
    private static final float[][] UV = {{0,1},{0,0},{1,0},{1,1}};
    private static final RackPlateMesh[][] PLATES = buildPlates();
    // Sixteen quads per ring: eight end pieces, four outer edges and four hole walls.
    private final float[] vertices = new float[16 * 4 * 8];
    private int cursor;

    private RackPlateMesh(float[][] ring) {
        float[] outer = ring[0].clone();
        for (float[] part : ring) {
            for (int axis = 0; axis < 3; axis++) {
                outer[axis] = Math.min(outer[axis], part[axis]);
                outer[axis + 3] = Math.max(outer[axis + 3], part[axis + 3]);
            }
        }
        int axis = 0;
        for (int i = 1; i < 3; i++) {
            if (outer[i + 3] - outer[i] < outer[axis + 3] - outer[axis]) axis = i;
        }
        int u = (axis + 1) % 3, v = (axis + 2) % 3;
        float[] hole = outer.clone();
        hole[u] = ring[2][u + 3]; hole[u + 3] = ring[3][u];
        hole[v] = ring[0][v + 3]; hole[v + 3] = ring[1][v];
        for (int face = 0; face < FACES.length; face++) {
            if (NORMALS[face][axis] != 0.0F) {
                for (float[] part : ring) addFace(part, face, false);
            } else {
                addFace(outer, face, false);
                addFace(hole, face, true);
            }
        }
        if (cursor != vertices.length) throw new IllegalStateException("Invalid rack ring mesh");
    }

    private static RackPlateMesh[][] buildPlates() {
        RackPlateMesh[][] result = new RackPlateMesh[5][EquipmentRackLayout.PAIRS_PER_PEG * 2];
        for (int slot = 0; slot < result.length; slot++) {
            for (int plate = 0; plate < result[slot].length; plate++) {
                result[slot][plate] = new RackPlateMesh(EquipmentRackLayout.plateBounds(slot, plate));
            }
        }
        return result;
    }

    private void addFace(float[] bounds, int face, boolean inward) {
        for (int i = 0; i < 4; i++) {
            int index = inward ? 3 - i : i;
            int[] point = FACES[face][index];
            vertices[cursor++] = bounds[point[0] * 3];
            vertices[cursor++] = bounds[1 + point[1] * 3];
            vertices[cursor++] = bounds[2 + point[2] * 3];
            vertices[cursor++] = UV[index][0];
            vertices[cursor++] = UV[index][1];
            float sign = inward ? -1.0F : 1.0F;
            for (float normal : NORMALS[face]) vertices[cursor++] = normal * sign;
        }
    }

    static void render(int slot, int count, MatrixStack matrices, VertexConsumer consumer,
                       Sprite sprite, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();
        float minU = sprite.getMinU(), rangeU = sprite.getMaxU() - minU;
        float minV = sprite.getMinV(), rangeV = sprite.getMaxV() - minV;
        int plates = Math.min(count, EquipmentRackLayout.PAIRS_PER_PEG) * 2;
        for (int plate = 0; plate < plates; plate++) {
            float[] data = PLATES[slot][plate].vertices;
            for (int i = 0; i < data.length; i += 8) {
                consumer.vertex(entry.getModel(), data[i], data[i + 1], data[i + 2])
                        .color(255, 255, 255, 255)
                        .texture(minU + data[i + 3] * rangeU, minV + data[i + 4] * rangeV)
                        .overlay(overlay).light(light)
                        .normal(entry.getNormal(), data[i + 5], data[i + 6], data[i + 7]).next();
            }
        }
    }
}
