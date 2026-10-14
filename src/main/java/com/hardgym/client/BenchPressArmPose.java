package com.hardgym.client;

import com.hardgym.animation.ExerciseAnimation;

/** Shared continuous shoulder/elbow angles and the corresponding palm position. */
public final class BenchPressArmPose {
    private BenchPressArmPose() {}

    public static float elbowBend(float anim) {
        // ANIM 0 = top, 1 = halfway, 2 = chest/pinned position.
        return ExerciseAnimation.pose(anim, 0.06F, 0.90F, 1.55F);
    }

    public static float shoulderPitch(float anim) {
        // Rotate the upper arm back by half the elbow angle. The forearm bends
        // forward again, so the two arm segments stay aimed along the bar path.
        return straightShoulderPitch(anim) + elbowBend(anim) * 0.5F;
    }

    public static float straightShoulderPitch(float anim) {
        return ExerciseAnimation.pose(anim, -1.34F, -1.00F, -0.72F);
    }

    public static float rightShoulderYaw(float anim) {
        return ExerciseAnimation.pose(anim, -0.18F, -0.14F, -0.06F);
    }

    public static float rightShoulderRoll(float anim) {
        return ExerciseAnimation.pose(anim, 0.10F, 0.07F, 0.03F);
    }

    /** Palm centre in player-model pixels, after elbow and shoulder rotation. */
    public static float[] handGripModelPosition(float anim, boolean left) {
        double bend = -elbowBend(anim);
        double pitch = shoulderPitch(anim);
        double yaw = left ? -rightShoulderYaw(anim) : rightShoulderYaw(anim);
        double roll = left ? -rightShoulderRoll(anim) : rightShoulderRoll(anim);

        // The arm cuboid runs from Y=-2 to Y=10. Its elbow is at Y=4;
        // the grip is centred in the palm at Y=9, one pixel from the end.
        double x = left ? 1.0D : -1.0D;
        double y = 4.0D + 5.0D * Math.cos(bend);
        double z = 5.0D * Math.sin(bend);

        // ModelPart applies Rz * Ry * Rx, after the local BendyLib elbow bend.
        double pitchedY = y * Math.cos(pitch) - z * Math.sin(pitch);
        double pitchedZ = y * Math.sin(pitch) + z * Math.cos(pitch);
        double yawedX = x * Math.cos(yaw) + pitchedZ * Math.sin(yaw);
        double yawedZ = -x * Math.sin(yaw) + pitchedZ * Math.cos(yaw);
        double rolledX = yawedX * Math.cos(roll) - pitchedY * Math.sin(roll);
        double rolledY = yawedX * Math.sin(roll) + pitchedY * Math.cos(roll);

        return new float[]{(float)((left ? 5.0D : -5.0D) + rolledX),
                (float)(2.0D + rolledY), (float)(-2.0D + yawedZ)};
    }

    /** Recompute the bar from the interpolated arm, rather than blending three bar positions. */
    public static float[] barCenterBlockModel(float anim) {
        float[] hand = handGripModelPosition(anim, false);
        // Convert the supine player model to NORTH-facing block-model pixels.
        // Server anchor: Y=0.34, Z=0.5-1.16. Renderer: Y+=0.10, rotate X by
        // 90 degrees, player scale 0.9375, vanilla model-origin shift -1.501.
        // The renderer's +0.02 Z and post-rotation -0.02 Y cancel here.
        double scale = 0.9375D;
        return new float[]{8.0F,
                (float)(16.0D * (0.34D + 0.10D) - scale * hand[2]),
                (float)(16.0D * (0.5D - 1.16D) + scale * (16.0D * 1.501D - hand[1]))};
    }
}
