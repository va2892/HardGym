package com.hardgym.animation;

/** Continuous visual motion; rep timing and gameplay still belong to the server managers. */
public final class ExerciseAnimation {
    private ExerciseAnimation() { }

    public static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    /** Zero velocity and acceleration at both ends, including repeated-cycle seams. */
    public static float ease(float value) {
        float t = clamp(value, 0.0F, 1.0F);
        return clamp(t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F), 0.0F, 1.0F);
    }

    /** Bench/squat: lower, briefly hold at the bottom, rise, settle at the top. */
    public static float cyclePhase(float progressPercent) {
        float p = clamp(progressPercent / 100.0F, 0.0F, 1.0F);
        if (p < 0.45F) return 2.0F * ease(p / 0.45F);
        if (p < 0.55F) return 2.0F;
        if (p < 0.95F) return 2.0F * (1.0F - ease((p - 0.55F) / 0.40F));
        return 0.0F;
    }

    /** Deadlift progress is the height of the pull and reverses on release/return. */
    public static float pullPhase(float progressPercent) {
        return 2.0F * ease(progressPercent / 100.0F);
    }

    /**
     * Monotone cubic interpolation through the three accepted poses.
     * A shared middle tangent avoids a kink when the phase crosses 1, without overshoot.
     */
    public static float pose(float phase, float start, float middle, float end) {
        float p = clamp(phase, 0.0F, 2.0F);
        float first = middle - start, second = end - middle;
        float tangent = first * second <= 0.0F ? 0.0F
                : 2.0F * first * second / (first + second);
        return p <= 1.0F ? hermite(p, start, middle, first, tangent)
                : hermite(p - 1.0F, middle, end, tangent, second);
    }

    private static float hermite(float t, float a, float b, float da, float db) {
        float t2 = t * t, t3 = t2 * t;
        return (2.0F * t3 - 3.0F * t2 + 1.0F) * a
                + (t3 - 2.0F * t2 + t) * da
                + (-2.0F * t3 + 3.0F * t2) * b + (t3 - t2) * db;
    }
}
