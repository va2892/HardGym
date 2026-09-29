package com.hardgym.mixin.client;

import com.hardgym.block.BenchPressBlock;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.client.BenchClientState;
import com.hardgym.client.DeadliftClientState;
import com.hardgym.client.SquatClientState;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
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

        BenchClientState.BenchMatch bench = BenchClientState.findActiveBench(entity);
        if (bench != null) {
            int anim = bench.state.get(BenchPressBlock.ANIM);
            model.rightLeg.pitch = 0.08F;
            model.leftLeg.pitch = 0.08F;
            model.rightLeg.yaw = 0.0F;
            model.leftLeg.yaw = 0.0F;
            model.rightLeg.roll = 0.0F;
            model.leftLeg.roll = 0.0F;
            model.body.pitch = 0.0F;
            model.head.pitch = 0.0F;
            model.head.yaw = 0.0F;
            if (anim == 2) {
                model.rightArm.pitch = -0.72F; model.leftArm.pitch = -0.72F;
                model.rightArm.yaw = -0.06F; model.leftArm.yaw = 0.06F;
                model.rightArm.roll = 0.03F; model.leftArm.roll = -0.03F;
            } else if (anim == 1) {
                model.rightArm.pitch = -1.00F; model.leftArm.pitch = -1.00F;
                model.rightArm.yaw = -0.14F; model.leftArm.yaw = 0.14F;
                model.rightArm.roll = 0.07F; model.leftArm.roll = -0.07F;
            } else {
                model.rightArm.pitch = -1.34F; model.leftArm.pitch = -1.34F;
                model.rightArm.yaw = -0.18F; model.leftArm.yaw = 0.18F;
                model.rightArm.roll = 0.10F; model.leftArm.roll = -0.10F;
            }
            return;
        }

        DeadliftClientState.DeadliftMatch deadlift = DeadliftClientState.findActiveDeadlift(entity);
        if (deadlift != null) {
            int anim = deadlift.state.get(DeadliftBarBlock.ANIM);

            // Keep the working v0.8.2 leg/knee trajectory.
            // The real problem was that vanilla rotates the BODY around its top pivot,
            // so at a large forward lean the waist visually moves away from the hips.
            // We compensate the body/head/arm pivots so the torso behaves as if it rotates
            // around the HIP joint instead. That keeps the torso physically connected to the legs.
            model.rightArm.pivotX = -5.0F;
            model.leftArm.pivotX = 5.0F;
            model.rightArm.yaw = -0.08F;
            model.leftArm.yaw = 0.08F;
            model.rightArm.roll = 0.03F;
            model.leftArm.roll = -0.03F;
            model.head.yaw = 0.0F;

            float torsoPitch;

            if (anim == 0) {
                // Start from the floor.
                // User target for this phase: thigh roughly parallel to the floor,
                // with a clearly closed knee angle (~60 degrees via BendyLib knee bend).
                // So the upper leg must come much farther forward than before.
                // More hip hinge at the bottom. Arms use pitch 0 because vanilla arm pitch 0
                // is straight down relative to the world/model, which is what we want for a
                // deadlift grip. The hip-pivot compensation below keeps the torso connected.
                torsoPitch = 0.95F;
                model.body.pitch = torsoPitch;
                model.head.pitch = -0.42F;
                model.rightArm.pitch = 0.0F;
                model.leftArm.pitch = 0.0F;
                // Negative pitch sends the thighs toward the bar/front.
                // v0.8.5 used the opposite sign and kicked the legs backward.
                model.rightLeg.pitch = -1.30F;
                model.leftLeg.pitch = -1.30F;
                model.rightLeg.yaw = -0.06F;
                model.leftLeg.yaw = 0.06F;
            } else if (anim == 1) {
                // Mid pull / bar around knee height.
                // Target silhouette from the user's sketch:
                // - shins close to vertical,
                // - hips still back,
                // - torso hinged forward,
                // - arms hanging straight down over the bar.
                torsoPitch = 0.70F;
                model.body.pitch = torsoPitch;
                model.head.pitch = -0.30F;
                model.rightArm.pitch = 0.0F;
                model.leftArm.pitch = 0.0F;
                model.rightLeg.pitch = -0.55F;
                model.leftLeg.pitch = -0.55F;
                model.rightLeg.yaw = -0.04F;
                model.leftLeg.yaw = 0.04F;
            } else {
                // Lockout.
                torsoPitch = 0.02F;
                model.body.pitch = torsoPitch;
                model.head.pitch = 0.0F;

                // Keep the shoulder in place and rotate the straight arm itself forward
                // toward the bar, matching the user's red-line sketch (~16 degrees).
                model.rightArm.pitch = -0.28F;
                model.leftArm.pitch = -0.28F;

                model.rightLeg.pitch = 0.0F;
                model.leftLeg.pitch = 0.0F;
                model.rightLeg.yaw = -0.02F;
                model.leftLeg.yaw = 0.02F;
            }

            // Vanilla torso is 12 model pixels tall. When it is pitched around its top,
            // the lower end no longer meets the leg pivots. Shift the whole upper-body assembly
            // by the opposite displacement so the waist remains anchored at the hips.
            float torsoLen = 12.0F;
            float hipFixY = torsoLen * (1.0F - (float)Math.cos(torsoPitch));
            float hipFixZ = -torsoLen * (float)Math.sin(torsoPitch);

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
        if (squat == null) return;
        int anim = squat.state.get(SquatRackBlock.ANIM);

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

        if (anim == 2) {
            model.body.pitch = 0.34F;
            model.head.pitch = -0.18F;
            model.rightLeg.pitch = 1.02F;
            model.leftLeg.pitch = 1.02F;
            model.rightLeg.yaw = -0.10F;
            model.leftLeg.yaw = 0.10F;
        } else if (anim == 1) {
            model.body.pitch = 0.19F;
            model.head.pitch = -0.10F;
            model.rightLeg.pitch = 0.55F;
            model.leftLeg.pitch = 0.55F;
            model.rightLeg.yaw = -0.06F;
            model.leftLeg.yaw = 0.06F;
        } else {
            model.body.pitch = 0.04F;
            model.head.pitch = 0.0F;
            model.rightLeg.pitch = 0.04F;
            model.leftLeg.pitch = 0.04F;
            model.rightLeg.yaw = -0.03F;
            model.leftLeg.yaw = 0.03F;
        }
        model.rightLeg.roll = 0.0F;
        model.leftLeg.roll = 0.0F;
    }
}
