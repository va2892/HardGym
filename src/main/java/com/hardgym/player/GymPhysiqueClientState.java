package com.hardgym.player;

/** Client-side cache populated by a small S2C packet. No Minecraft client classes here, so it is safe to load on a server. */
public final class GymPhysiqueClientState {
    private static volatile int strengthLevel = 1;
    private static volatile int benchLevel = 1;
    private static volatile int squatLevel = 1;
    private static volatile int deadliftLevel = 1;

    private GymPhysiqueClientState() { }

    public static void update(int strength, int bench, int squat, int deadlift) {
        strengthLevel = Math.max(1, strength);
        benchLevel = Math.max(1, bench);
        squatLevel = Math.max(1, squat);
        deadliftLevel = Math.max(1, deadlift);
    }

    public static int getStrengthLevel() { return strengthLevel; }
    public static int getBenchLevel() { return benchLevel; }
    public static int getSquatLevel() { return squatLevel; }
    public static int getDeadliftLevel() { return deadliftLevel; }
}
