package com.hardgym.player;

/**
 * Strength-oriented %1RM -> reps model used by barbell exercises.
 * Values mirror the user's calculator strength column:
 * 100%=1, 95%=1-2, 90%=2-3, 85%=4-5, 80%=6-7, 75%=8-9,
 * 70%=10-11, 65%=12-17, 60%=18-25, 55%=26-29, 50%=30-37.
 */
public final class StrengthRepTable {
    private StrengthRepTable() { }

    /** Mid/upper representative rep target for the strength column. */
    public static int targetReps(double percentOneRm) {
        if (percentOneRm >= 97.5D) return 1;
        if (percentOneRm >= 92.5D) return 2;
        if (percentOneRm >= 87.5D) return 3;
        if (percentOneRm >= 82.5D) return 5;
        if (percentOneRm >= 77.5D) return 7;
        if (percentOneRm >= 72.5D) return 9;
        if (percentOneRm >= 67.5D) return 11;
        if (percentOneRm >= 62.5D) return 15;
        if (percentOneRm >= 57.5D) return 22;
        if (percentOneRm >= 52.5D) return 28;
        return 34;
    }

    public static int targetReps(int weightKg, double oneRepMaxKg) {
        if (oneRepMaxKg <= 0.0D) return 5;
        return targetReps(weightKg * 100.0D / oneRepMaxKg);
    }

    /**
     * Fatigue is a between-set penalty. The current set snapshots its rep target when it starts,
     * so fatigue accumulated during that set mainly reduces the NEXT set, not the current one.
     * 0 FAT = full calculator target; 30 FAT ~= 75%; 60 FAT ~= 50%; 90 FAT ~= 25%.
     */
    public static int targetRepsWithFatigue(int weightKg, double oneRepMaxKg, int fatigue) {
        int base = targetReps(weightKg, oneRepMaxKg);
        int f = Math.max(0, Math.min(100, fatigue));
        double multiplier = Math.max(0.20D, 1.0D - f * 0.0085D);
        return Math.max(1, (int)Math.round(base * multiplier));
    }

    /**
     * A complete hard set now builds about 10 FAT instead of ~30.
     * The 10 points are distributed across the reps, so FAT still rises while the
     * set is happening, including very high-rep sets where integer FAT cannot be
     * added every single rep. A completed set reaches exactly 10 FAT from reps.
     *
     * @param setTargetReps target reps snapshotted at the start of the set
     * @param completedRepsBefore number of reps already completed before this rep
     */
    public static int fatigueGainForRep(int setTargetReps, int completedRepsBefore) {
        int target = Math.max(1, setTargetReps);
        int beforeReps = Math.max(0, Math.min(target, completedRepsBefore));
        int afterReps = Math.min(target, beforeReps + 1);

        int beforeFat = (beforeReps * 10) / target;
        int afterFat = (afterReps * 10) / target;
        return Math.max(0, afterFat - beforeFat);
    }

    /**
     * Reps consume about 90% of a full stamina bar by the calculator target.
     * A rested lifter therefore reaches the table target, while pauses/low stamina
     * can make a set fail earlier.
     */
    public static int staminaCostForTarget(int targetReps) {
        return Math.max(1, (int)Math.ceil(90.0D / Math.max(1, targetReps)));
    }

    /** Estimate 1RM from a completed set using the same table. */
    public static double estimateOneRepMax(int weightKg, int reps) {
        if (reps <= 0) return 0.0D;
        double fraction;
        if (reps <= 1) fraction = 1.00D;
        else if (reps <= 2) fraction = 0.95D;
        else if (reps <= 3) fraction = 0.90D;
        else if (reps <= 5) fraction = 0.85D;
        else if (reps <= 7) fraction = 0.80D;
        else if (reps <= 9) fraction = 0.75D;
        else if (reps <= 11) fraction = 0.70D;
        else if (reps <= 17) fraction = 0.65D;
        else if (reps <= 25) fraction = 0.60D;
        else if (reps <= 29) fraction = 0.55D;
        else fraction = 0.50D;
        return weightKg / fraction;
    }

    /** Old PRs did not record reps, so migration treats them as a roughly 5RM set. */
    public static double migrateOldPr(int prKg) {
        return prKg <= 0 ? 0.0D : prKg / 0.85D;
    }

    public static int roundedKg(double value) {
        return Math.max(0, (int)Math.round(value));
    }
}
