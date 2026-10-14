package com.hardgym.player;

/** Muscle levels used for working weights, repetition targets and training tempo. */
public final class GymMuscleStrength {
    private GymMuscleStrength() { }

    /** Bench press trains both chest and triceps; use their average level, rounded down. */
    public static int benchLevel(GymPlayerData data) {
        int chest = GymStats.levelForXp(data.hardgym$getChestXp());
        int triceps = GymStats.levelForXp(data.hardgym$getTricepsXp());
        return (chest + triceps) / 2;
    }

    /** Squats and deadlifts both train the existing Legs muscle group. */
    public static int legsLevel(GymPlayerData data) {
        return GymStats.levelForXp(data.hardgym$getLegsXp());
    }
}
