package com.hardgym.player;

import java.util.Arrays;

/** Ordered plate pairs; all exercise calculations use only their summed mass. */
public final class BarbellLoad {
    public static final int BAR_WEIGHT_KG = 20;
    private static final int[] PLATE_WEIGHTS_KG = {10, 15, 20, 25, 50};
    private final int capacity;
    private final int[] pairs;
    private int size;

    public BarbellLoad(int capacity) {
        if (capacity < 1 || capacity > 9) throw new IllegalArgumentException("Invalid plate capacity");
        this.capacity = capacity;
        this.pairs = new int[capacity];
    }

    public static boolean isPlateWeight(int weightKg) {
        for (int weight : PLATE_WEIGHTS_KG) if (weight == weightKg) return true;
        return false;
    }

    public boolean addPair(int weightKg) {
        if (size == capacity || !isPlateWeight(weightKg)) return false;
        pairs[size++] = weightKg;
        return true;
    }

    public int getWeightKg() {
        int result = BAR_WEIGHT_KG;
        for (int i = 0; i < size; i++) result += 2 * pairs[i];
        return result;
    }

    public int size() {
        return size;
    }

    public int[] getPairs() {
        return Arrays.copyOf(pairs, size);
    }

    /** Remove the outermost pair, which was installed last; zero means the bar is empty. */
    public int removeLast() {
        if (size == 0) return 0;
        int removed = pairs[--size];
        pairs[size] = 0;
        return removed;
    }

    public int[] removeAll() {
        int[] removed = getPairs();
        Arrays.fill(pairs, 0);
        size = 0;
        return removed;
    }

    public static BarbellLoad fromSavedPairs(int capacity, int[] saved) {
        BarbellLoad result = new BarbellLoad(capacity);
        if (saved != null) {
            for (int weight : saved) {
                if (result.size == capacity) break;
                result.addPair(weight);
            }
        }
        return result;
    }

    /** Reachable totals with up to capacity pairs, including the empty bar. */
    public static int[] reachableWeights(int capacity) {
        int slots = new BarbellLoad(capacity).capacity;
        int max = slots * 50;
        boolean[][] reachable = new boolean[slots + 1][max + 1];
        reachable[0][0] = true;
        for (int count = 0; count < slots; count++) {
            for (int sum = 0; sum <= max; sum++) {
                if (!reachable[count][sum]) continue;
                for (int weight : PLATE_WEIGHTS_KG) {
                    if (sum + weight <= max) reachable[count + 1][sum + weight] = true;
                }
            }
        }
        int[] weights = new int[max + 1];
        int size = 0;
        for (int sum = 0; sum <= max; sum++) {
            for (int count = 0; count <= slots; count++) {
                if (reachable[count][sum]) {
                    weights[size++] = BAR_WEIGHT_KG + 2 * sum;
                    break;
                }
            }
        }
        return Arrays.copyOf(weights, size);
    }
}
