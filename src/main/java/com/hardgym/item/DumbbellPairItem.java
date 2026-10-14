package com.hardgym.item;

/** One inventory item is a pair; weight is for EACH dumbbell. */
public final class DumbbellPairItem extends DumbbellItem {
    public DumbbellPairItem(int weightKg, Settings settings) {
        super(weightKg, requiredLevelForWeight(weightKg), weightKg * 4 / 5,
                weightKg * 4 / 5 + 2, weightKg == 10 ? 8 : 14 + (weightKg - 20) / 5,
                weightKg == 10 ? 8 : 10 + (weightKg - 20) / 10,
                weightKg == 10 ? 48 : 56 + (weightKg - 20) / 5, settings);
        if (weightKg < 10 || weightKg > 120 || weightKg % 10 != 0) {
            throw new IllegalArgumentException("Dumbbell pair weight must be 10..120 kg in steps of 10");
        }
    }

    public static int requiredLevelForWeight(int weightKg) {
        return weightKg == 10 ? 1 : 5 + (weightKg - 20) / 5;
    }

    @Override
    public int getDumbbellCount() {
        return 2;
    }
}
