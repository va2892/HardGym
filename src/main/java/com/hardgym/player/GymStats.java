package com.hardgym.player;

public final class GymStats {
    private GymStats() {
    }

    public static int levelForXp(int xp) {
        if (xp <= 0) {
            return 1;
        }
        return 1 + (int) Math.floor(Math.sqrt(xp / 50.0D));
    }

    public static int xpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        int n = level - 1;
        return 50 * n * n;
    }

    public static int xpToNextLevel(int xp) {
        int level = levelForXp(xp);
        return Math.max(0, xpForLevel(level + 1) - xp);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
