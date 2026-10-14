package com.hardgym.client;

import com.hardgym.animation.ExerciseAnimation;

/** Model-space cuboids shared by the renderer and the resource generator (16 units per block). */
public final class BarbellGeometry {
    public static final float PLATE_THICKNESS = 1.10F;
    public static final float PLATE_SPACING = 1.18F;

    private BarbellGeometry() {}

    private static float[] center(int capacity, float phase, boolean active, int pairCount) {
        float p = ExerciseAnimation.clamp(phase, 0.0F, 2.0F);
        if (capacity == 5) {
            if (!active) return new float[]{15.5F - 2.5F * p, 12.5F};
            float[] grip = BenchPressArmPose.barCenterBlockModel(p);
            return new float[]{grip[1], grip[2]};
        }
        if (capacity == 7) {
            if (!active) return new float[]{22.0F - 4.0F * p, 9.5F};
            float[] grip = StandingBarbellPose.squatBarCenterBlockModel(p);
            return new float[]{grip[1], grip[2]};
        }
        if (capacity == 9) {
            if (!active) return new float[]{pairCount == 0 ? 0.5F : 3.0F, 8.0F};
            float[] grip = StandingBarbellPose.deadliftBarCenterBlockModel(p);
            return new float[]{grip[1], grip[2]};
        }
        throw new IllegalArgumentException("Invalid station capacity");
    }

    public static float[] barBounds(int capacity, float phase, boolean active, int pairCount) {
        float[] center = center(capacity, phase, active, pairCount);
        float minX = capacity == 5 ? -6.0F : capacity == 7 ? -9.0F : -12.0F;
        float halfZ = capacity == 5 ? 0.5F : 1.0F;
        return new float[]{minX, center[0] - 0.5F, center[1] - halfZ,
                16.0F - minX, center[0] + 0.5F, center[1] + halfZ};
    }

    public static float[] plateBounds(int capacity, int index, boolean left, float phase,
                                      boolean active, int pairCount) {
        if (index < 0 || index >= capacity) throw new IllegalArgumentException("Invalid plate index");
        float[] center = center(capacity, phase, active, pairCount);
        float leftX = (capacity == 5 ? -0.55F : -0.95F) - index * PLATE_SPACING;
        float x = left ? leftX : 16.0F - leftX;
        return new float[]{x - PLATE_THICKNESS / 2, center[0] - 3.0F, center[1] - 3.0F,
                x + PLATE_THICKNESS / 2, center[0] + 3.0F, center[1] + 3.0F};
    }

    public static String plateColor(int weightKg) {
        switch (weightKg) {
            case 10: return "green";
            case 15: return "yellow";
            case 20: return "blue";
            case 25: return "red";
            case 50: return "black";
            default: throw new IllegalArgumentException("Invalid plate weight");
        }
    }
}
