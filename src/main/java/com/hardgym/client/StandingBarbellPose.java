package com.hardgym.client;

import com.hardgym.animation.ExerciseAnimation;

/** Shared upper-body motion and grip positions for squat and deadlift rendering. */
public final class StandingBarbellPose {
    public static final float PLAYER_SCALE = 0.9375F;
    public static final float DEADLIFT_RIGHT_ARM_YAW = -0.08F;
    public static final float DEADLIFT_RIGHT_ARM_ROLL = 0.03F;

    private StandingBarbellPose() { }

    public static float deadliftTorsoPitch(float phase) {
        return ExerciseAnimation.pose(phase, 0.95F, 0.70F, 0.02F);
    }

    public static float deadliftArmPitch(float phase) {
        return ExerciseAnimation.pose(phase, 0.0F, 0.0F, -0.28F);
    }

    public static float deadliftBodyOffset(float phase) {
        return ExerciseAnimation.pose(phase, -0.28F, -0.06F, 0.0F);
    }

    public static float squatTorsoPitch(float phase) {
        return ExerciseAnimation.pose(phase, 0.04F, 0.19F, 0.34F);
    }

    public static float squatLegLift(float phase) {
        return ExerciseAnimation.pose(phase, 0.0F, 3.0F, 4.6F);
    }

    public static float squatBodyOffset(float phase) {
        return ExerciseAnimation.pose(phase, 0.0F, -0.25F, -0.50F);
    }

    private static float hipOffsetY(float torsoPitch) {
        return 12.0F * (1.0F - (float) Math.cos(torsoPitch));
    }

    private static float hipOffsetZ(float torsoPitch) {
        return -12.0F * (float) Math.sin(torsoPitch);
    }

    public static float deadliftUpperBodyY(float phase) {
        return hipOffsetY(deadliftTorsoPitch(phase));
    }

    public static float deadliftUpperBodyZ(float phase) {
        return hipOffsetZ(deadliftTorsoPitch(phase));
    }

    public static float squatUpperBodyY(float phase) {
        return hipOffsetY(squatTorsoPitch(phase)) - squatLegLift(phase);
    }

    public static float squatUpperBodyZ(float phase) {
        return hipOffsetZ(squatTorsoPitch(phase));
    }

    /** Palm centre in model pixels. Deadlift arms are straight, with a changing shoulder pitch. */
    public static float[] deadliftHandGripModelPosition(float phase, boolean left) {
        double pitch = deadliftArmPitch(phase);
        double yaw = left ? -DEADLIFT_RIGHT_ARM_YAW : DEADLIFT_RIGHT_ARM_YAW;
        double roll = left ? -DEADLIFT_RIGHT_ARM_ROLL : DEADLIFT_RIGHT_ARM_ROLL;
        double x = left ? 1.0D : -1.0D;
        double pitchedY = 9.0D * Math.cos(pitch);
        double pitchedZ = 9.0D * Math.sin(pitch);
        double yawedX = x * Math.cos(yaw) + pitchedZ * Math.sin(yaw);
        double yawedZ = -x * Math.sin(yaw) + pitchedZ * Math.cos(yaw);
        double rolledX = yawedX * Math.cos(roll) - pitchedY * Math.sin(roll);
        double rolledY = yawedX * Math.sin(roll) + pitchedY * Math.cos(roll);
        return new float[]{(float) ((left ? 5.0D : -5.0D) + rolledX),
                (float) (2.0D + deadliftUpperBodyY(phase) + rolledY),
                (float) (deadliftUpperBodyZ(phase) + yawedZ)};
    }

    /** Keep the accepted floor grip, then follow both palms through the entire pull/return. */
    public static float[] deadliftBarCenterBlockModel(float phase) {
        float[] hand = deadliftHandGripModelPosition(phase, false);
        float[] floorHand = deadliftHandGripModelPosition(0.0F, false);
        return new float[]{8.0F,
                3.0F + 16.0F * (deadliftBodyOffset(phase) - deadliftBodyOffset(0.0F))
                        - PLAYER_SCALE * (hand[1] - floorHand[1]),
                8.0F + PLAYER_SCALE * (hand[2] - floorHand[2])};
    }

    /**
     * Squat elbows/shoulder angles stay fixed, so a palm's displacement equals its
     * shoulder pivot's displacement. Include the whole-player lowering as well.
     * The player faces away from the block's NORTH frame: model Z has the opposite sign.
     */
    public static float[] squatBarCenterBlockModel(float phase) {
        return new float[]{8.0F,
                22.0F + 16.0F * (squatBodyOffset(phase) - squatBodyOffset(0.0F))
                        - PLAYER_SCALE * (squatUpperBodyY(phase) - squatUpperBodyY(0.0F)),
                2.9F - PLAYER_SCALE * (squatUpperBodyZ(phase) - squatUpperBodyZ(0.0F))};
    }
}
