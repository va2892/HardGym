package com.hardgym.mixin.client;

import com.hardgym.animation.ExerciseAnimation;
import com.hardgym.client.BenchClientState;
import com.hardgym.client.BenchPressArmPose;
import com.hardgym.client.DeadliftClientState;
import com.hardgym.client.DumbbellArmPose;
import com.hardgym.client.ExerciseClientState;
import com.hardgym.client.OptionalPlayerAnimator;
import com.hardgym.client.SquatClientState;
import com.hardgym.client.StandingBarbellPose;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void hardgym$animateTraining(T entity, float limbAngle, float limbDistance,
                                         float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        BipedEntityModel<?> model = (BipedEntityModel<?>)(Object)this;
        float tickDelta = ExerciseAnimation.clamp(animationProgress - entity.age, 0.0F, 1.0F);

        BenchClientState.BenchMatch bench = BenchClientState.findActiveBench(entity);
        if (bench != null) {
            float anim = ExerciseClientState.phase(entity.world, bench.pos, bench.state, tickDelta);
            // Keep the hips connected to the torso and spread the thighs with roll.
            // Add a small bench-edge gap by moving each leg 0.25 model pixels outward.
            // Preserve the accepted thigh rotation and knee bend from v0.10.28.
            // OptionalPlayerAnimator supplies the knee bend when BendyLib is installed.
            model.rightLeg.pivotX = -3.0F;
            model.leftLeg.pivotX = 3.0F;
            model.rightLeg.pitch = 0.14F;
            model.leftLeg.pitch = 0.14F;
            model.rightLeg.yaw = 0.0F;
            model.leftLeg.yaw = 0.0F;
            model.rightLeg.roll = 0.40F;
            model.leftLeg.roll = -0.40F;

            // Restore the base bench position from v0.10.12 and lift only the upper body
            // away from the bench surface. Because the whole player is rotated flat in the
            // renderer, local Z corresponds to vertical separation from the bench here.
            float benchBodyLiftZ = -2.0F;
            model.body.pivotZ = benchBodyLiftZ;
            model.head.pivotZ = benchBodyLiftZ;
            model.rightArm.pivotZ = benchBodyLiftZ;
            model.leftArm.pivotZ = benchBodyLiftZ;
            // Restore the bench shoulder anchors on every frame, including after
            // rendering a squat/deadlift pose with shifted shoulder pivots.
            model.rightArm.pivotX = -5.0F;
            model.leftArm.pivotX = 5.0F;
            model.rightArm.pivotY = 2.0F;
            model.leftArm.pivotY = 2.0F;

            // Raise both legs by the same amount so the hips stay aligned with the
            // already-raised torso instead of hanging lower relative to the body.
            model.rightLeg.pivotZ = benchBodyLiftZ;
            model.leftLeg.pivotZ = benchBodyLiftZ;

            model.body.pitch = 0.0F;
            model.head.pitch = 0.0F;
            model.head.yaw = 0.0F;
            // The bend layer changes the forearm. Compensate at the shoulder only
            // when both optional libraries are available; otherwise keep the rigid pose.
            float shoulderPitch = OptionalPlayerAnimator.hasBendLayer(entity)
                    ? BenchPressArmPose.shoulderPitch(anim) : BenchPressArmPose.straightShoulderPitch(anim);
            model.rightArm.pitch = shoulderPitch;
            model.leftArm.pitch = shoulderPitch;
            model.rightArm.yaw = BenchPressArmPose.rightShoulderYaw(anim);
            model.leftArm.yaw = -model.rightArm.yaw;
            model.rightArm.roll = BenchPressArmPose.rightShoulderRoll(anim);
            model.leftArm.roll = -model.rightArm.roll;
            return;
        }

        DeadliftClientState.DeadliftMatch deadlift = DeadliftClientState.findActiveDeadlift(entity);
        if (deadlift != null) {
            float anim = ExerciseClientState.phase(entity.world, deadlift.pos, deadlift.state, tickDelta);

            // Keep the working v0.8.2 leg/knee trajectory.
            // The real problem was that vanilla rotates the BODY around its top pivot,
            // so at a large forward lean the waist visually moves away from the hips.
            // We compensate the body/head/arm pivots so the torso behaves as if it rotates
            // around the HIP joint instead. That keeps the torso physically connected to the legs.
            model.rightArm.pivotX = -5.0F;
            model.leftArm.pivotX = 5.0F;
            model.rightArm.yaw = StandingBarbellPose.DEADLIFT_RIGHT_ARM_YAW;
            model.leftArm.yaw = -model.rightArm.yaw;
            model.rightArm.roll = StandingBarbellPose.DEADLIFT_RIGHT_ARM_ROLL;
            model.leftArm.roll = -model.rightArm.roll;
            model.head.yaw = 0.0F;

            float torsoPitch = StandingBarbellPose.deadliftTorsoPitch(anim);
            model.body.pitch = torsoPitch;
            model.head.pitch = ExerciseAnimation.pose(anim, -0.42F, -0.30F, 0.0F);
            model.rightArm.pitch = StandingBarbellPose.deadliftArmPitch(anim);
            model.leftArm.pitch = model.rightArm.pitch;
            model.rightLeg.pitch = ExerciseAnimation.pose(anim, -1.30F, -0.55F, 0.0F);
            model.leftLeg.pitch = model.rightLeg.pitch;
            model.rightLeg.yaw = ExerciseAnimation.pose(anim, -0.06F, -0.04F, -0.02F);
            model.leftLeg.yaw = -model.rightLeg.yaw;

            // Vanilla torso is 12 model pixels tall. When it is pitched around its top,
            // the lower end no longer meets the leg pivots. Shift the whole upper-body assembly
            // by the opposite displacement so the waist remains anchored at the hips.
            float hipFixY = StandingBarbellPose.deadliftUpperBodyY(anim);
            float hipFixZ = StandingBarbellPose.deadliftUpperBodyZ(anim);

            model.body.pivotY = hipFixY;
            model.body.pivotZ = hipFixZ;

            model.head.pivotY = hipFixY;
            model.head.pivotZ = hipFixZ;

            model.rightArm.pivotY = 2.0F + hipFixY;
            model.leftArm.pivotY = 2.0F + hipFixY;

            // Shoulder stays at its normal compensated pivot. At lockout the ARM itself
            // is rotated forward above; do not translate the shoulder.
            model.rightArm.pivotZ = hipFixZ;
            model.leftArm.pivotZ = hipFixZ;

            model.rightLeg.roll = 0.0F;
            model.leftLeg.roll = 0.0F;
            return;
        }

        SquatClientState.SquatMatch squat = SquatClientState.findActiveSquat(entity);
        if (squat == null) {
            if (DumbbellArmPose.isCurling(entity)) {
                float lift = DumbbellArmPose.lift(entity, tickDelta);
                // A pair shares one timer, so both arms lift and lower together.
                if (DumbbellArmPose.usesArm(entity, Arm.RIGHT)) {
                    model.rightArm.pitch = DumbbellArmPose.shoulderPitch(lift, OptionalPlayerAnimator.hasBendLayer(entity));
                    model.rightArm.yaw = 0.0F;
                    model.rightArm.roll = 0.05F;
                    model.rightArm.pivotX = -5.0F;
                    model.rightArm.pivotZ = 0.0F;
                }
                if (DumbbellArmPose.usesArm(entity, Arm.LEFT)) {
                    model.leftArm.pitch = DumbbellArmPose.shoulderPitch(lift, OptionalPlayerAnimator.hasBendLayer(entity));
                    model.leftArm.yaw = 0.0F;
                    model.leftArm.roll = -0.05F;
                    model.leftArm.pivotX = 5.0F;
                    model.leftArm.pivotZ = 0.0F;
                }
            }
            return;
        }
        float anim = ExerciseClientState.phase(entity.world, squat.pos, squat.state, tickDelta);

        // Keep the v0.7.9 squat arm/elbow geometry, but retract the shoulders:
        // move both shoulder pivots a little backward and slightly inward, like squeezing
        // the shoulder blades together under a barbell.
        model.rightArm.pitch = 0.16F;
        model.leftArm.pitch = 0.16F;
        model.rightArm.yaw = -0.18F;
        model.leftArm.yaw = 0.18F;
        model.rightArm.roll = 1.30F;
        model.leftArm.roll = -1.30F;

        // Vanilla shoulder pivots are around X = +/-5, Z = 0.
        // Positive Z is toward the player's back in the model coordinate system.
        model.rightArm.pivotX = -3.65F;
        model.leftArm.pivotX = 3.65F;
        model.rightArm.pivotZ = 3.75F;
        model.leftArm.pivotZ = 3.75F;

        model.head.yaw = 0.0F;

        // With real BendyLib knees the thigh must rotate toward the FRONT of the
        // player. In this model coordinate system that is NEGATIVE pitch (same rule
        // already proven by the deadlift). The old positive values were only useful
        // when the whole leg was rigid and make a bent squat look backwards.
        //
        // Combined with the positive BEND values from OptionalPlayerAnimator:
        //   bottom: thigh -1.02 + knee bend 1.45 -> knee forward, shin back toward foot
        //   middle: thigh -0.55 + knee bend 0.72 -> smaller natural knee angle
        float torsoPitch = StandingBarbellPose.squatTorsoPitch(anim);
        model.body.pitch = torsoPitch;
        model.head.pitch = ExerciseAnimation.pose(anim, 0.0F, -0.10F, -0.18F);
        model.rightLeg.pitch = ExerciseAnimation.pose(anim, -0.04F, -0.55F, -1.02F);
        model.leftLeg.pitch = model.rightLeg.pitch;
        model.rightLeg.yaw = ExerciseAnimation.pose(anim, -0.03F, -0.06F, -0.10F);
        model.leftLeg.yaw = -model.rightLeg.yaw;

        // Ground-contact correction for the squat: raise ONLY the legs.
        // v0.10.3 achieved the same visual foot correction by raising the entire player
        // (and the mounted bar), which also moved the torso/head/arms. Keep those parts
        // at their old height and move the two leg roots upward instead.
        // Model Y grows downward, so subtracting from pivotY moves the legs upward.
        float legLiftY = StandingBarbellPose.squatLegLift(anim);
        model.rightLeg.pivotY = 12.0F - legLiftY;
        model.leftLeg.pivotY = 12.0F - legLiftY;

        // Vanilla rotates the torso around its TOP pivot. Once we added real knee
        // bending and a forward torso lean, the bottom of the torso visually moved
        // away from the fixed hip/leg pivots, which made the legs look detached.
        //
        // Use the same proven hip-pivot compensation as the deadlift: move the whole
        // upper-body assembly by the opposite displacement of the torso's lower end.
        // This keeps the waist glued to the hips while preserving the squat angles.
        // The leg roots above are lifted to keep the feet out of the ground.
        // Move ONLY the upper-body model parts by the same amount so the waist no
        // longer protrudes through the raised thighs. This does not touch the leg
        // pivots again and does not use a whole-player MatrixStack translation.
        // The bar renderer reads these same upper-body pivots to keep the grip attached.
        float upperBodyY = StandingBarbellPose.squatUpperBodyY(anim);
        float upperBodyZ = StandingBarbellPose.squatUpperBodyZ(anim);

        model.body.pivotY = upperBodyY;
        model.body.pivotZ = upperBodyZ;

        model.head.pivotY = upperBodyY;
        model.head.pivotZ = upperBodyZ;

        // Keep the existing squeezed-back squat shoulder position, but move it with
        // the compensated torso so the arms and bar stay attached to the body too.
        model.rightArm.pivotY = 2.0F + upperBodyY;
        model.leftArm.pivotY = 2.0F + upperBodyY;
        model.rightArm.pivotZ = 3.75F + upperBodyZ;
        model.leftArm.pivotZ = 3.75F + upperBodyZ;

        model.rightLeg.roll = 0.0F;
        model.leftLeg.roll = 0.0F;
    }
}
