package com.hardgym.player;

public final class GymPhysicalStats {
    private GymPhysicalStats() { }

    // Vanilla player base values in 1.16.5: damage 1.0, movement speed 0.10,
    // jump velocity 0.42, max health 20 (= 10 hearts).

    /** Bench press progression -> base attack damage. */
    public static double attackDamageForBenchLevel(int level) {
        double value = 0.50D + (Math.max(1, level) - 1) * 0.20D;
        return Math.min(6.0D, value);
    }

    /** Squat progression -> movement speed attribute. */
    public static double movementSpeedForSquatLevel(int level) {
        double value = 0.085D + (Math.max(1, level) - 1) * 0.003D;
        return Math.min(0.170D, value);
    }

    /** Deadlift progression -> jump launch velocity.
     *  Level 1 starts at the vanilla Minecraft value (0.42 = 100%).
     *  Deadlift progression then increases jump power above vanilla.
     */
    public static float jumpVelocityForDeadliftLevel(int level) {
        float value = 0.42F + (Math.max(1, level) - 1) * 0.008F;
        return Math.min(0.56F, value);
    }

    /** Overall Strength -> maximum health. 2 health points = 1 heart. */
    public static double maxHealthForStrengthLevel(int level) {
        double value = 16.0D + (Math.max(1, level) - 1) * 1.0D;
        return Math.min(40.0D, value);
    }

    public static int runSpeedPercent(int squatLevel) {
        return (int)Math.round(movementSpeedForSquatLevel(squatLevel) / 0.10D * 100.0D);
    }

    public static int jumpHeightPercent(int deadliftLevel) {
        double ratio = jumpVelocityForDeadliftLevel(deadliftLevel) / 0.42D;
        return (int)Math.round(ratio * ratio * 100.0D);
    }
}
