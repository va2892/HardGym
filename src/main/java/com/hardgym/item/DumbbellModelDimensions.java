package com.hardgym.item;

/** Generated from the paired-dumbbell models by tools/generate_dumbbell_pairs.py. */
public final class DumbbellModelDimensions {
    private static final float[] HALF_HEIGHTS = {3F, 3.5F, 3.59F, 3.68F, 3.77F, 3.86F, 3.95F, 4.04F, 4.13F, 4.22F, 4.31F, 4.4F};
    private static final float[] HALF_LENGTHS = {7.8F, 7.8F, 8.92F, 8.92F, 10.04F, 10.04F, 11.16F, 11.16F, 12.93F, 12.93F, 14.7F, 14.7F};

    private DumbbellModelDimensions() { }

    private static int index(int weight) {
        return Math.max(0, Math.min(HALF_HEIGHTS.length - 1, weight / 10 - 1));
    }

    public static float halfHeight(int weight) {
        return HALF_HEIGHTS[index(weight)];
    }

    public static float halfLength(int weight) {
        return HALF_LENGTHS[index(weight)];
    }
}
