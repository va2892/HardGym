package com.hardgym.player;

public final class GymPhysicalStats {
    private GymPhysicalStats() { }

    // Vanilla player base values in 1.16.5: damage 1.0, movement speed 0.10,
    // jump velocity 0.42, max health 20 (= 10 hearts).
    private static final float MAX_JUMP_VELOCITY = 0.42F * (float)Math.sqrt(2.80D);

    /** Bench press: keep the old progression through level 29, then reach 10 at 40. */
    public static double attackDamageForBenchLevel(int level) {
        int boundedLevel = Math.max(1, Math.min(40, level));
        if (boundedLevel <= 29) {
            double value = 0.50D + (boundedLevel - 1) * 0.20D;
            return Math.min(6.0D, value);
        }
        double progress = (boundedLevel - 29) / 11.0D;
        return 6.0D + (10.0D - 6.0D) * progress;
    }

    /** Squat: keep the old progression through level 30, then reach 270% at 40. */
    public static double movementSpeedForSquatLevel(int level) {
        int boundedLevel = Math.max(1, Math.min(40, level));
        if (boundedLevel <= 30) {
            double value = 0.085D + (boundedLevel - 1) * 0.003D;
            return Math.min(0.170D, value);
        }
        double progress = (boundedLevel - 30) / 10.0D;
        return 0.170D + (0.270D - 0.170D) * progress;
    }

    /** Deadlift progression -> jump launch velocity.
     *  Level 1 starts at the vanilla Minecraft value (0.42 = 100%).
     *  Keep the existing curve to 0.56 at level 30, then reach the displayed
     *  280% jump-height estimate at level 40.
     */
    public static float jumpVelocityForDeadliftLevel(int level) {
        int boundedLevel = Math.max(1, Math.min(40, level));
        if (boundedLevel <= 30) {
            float progress = (boundedLevel - 1) / 29.0F;
            return 0.42F + (0.56F - 0.42F) * progress;
        }
        float progress = (boundedLevel - 30) / 10.0F;
        return 0.56F + (MAX_JUMP_VELOCITY - 0.56F) * progress;
    }

    /** Strength: keep 20 hearts at level 25, then reach 30 at 35. 2 HP = 1 heart. */
    public static double maxHealthForStrengthLevel(int level) {
        int boundedLevel = Math.max(1, Math.min(35, level));
        if (boundedLevel <= 25) {
            return 16.0D + (boundedLevel - 1) * 1.0D;
        }
        double progress = (boundedLevel - 25) / 10.0D;
        return 40.0D + (60.0D - 40.0D) * progress;
    }

    /** Biceps: 80% at level 1, vanilla at 5, 150% at 15, then 220% at 25. */
    public static float miningSpeedMultiplierForBicepsLevel(int level) {
        int boundedLevel = Math.max(1, Math.min(25, level));
        if (boundedLevel <= 15) {
            return 0.80F + (boundedLevel - 1) * 0.05F;
        }
        float progress = (boundedLevel - 15) / 10.0F;
        return 1.50F + (2.20F - 1.50F) * progress;
    }

    public static int miningSpeedPercent(int bicepsLevel) {
        return Math.round(miningSpeedMultiplierForBicepsLevel(bicepsLevel) * 100.0F);
    }

    public static int runSpeedPercent(int squatLevel) {
        return (int)Math.round(movementSpeedForSquatLevel(squatLevel) / 0.10D * 100.0D);
    }

    public static int jumpHeightPercent(int deadliftLevel) {
        double ratio = jumpVelocityForDeadliftLevel(deadliftLevel) / 0.42D;
        return (int)Math.round(ratio * ratio * 100.0D);
    }
}
