package com.hardgym.mixin.client;

import com.hardgym.block.BenchPressBlock;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.client.BenchClientState;
import com.hardgym.client.DeadliftClientState;
import com.hardgym.client.ExerciseClientState;
import com.hardgym.client.SquatClientState;
import com.hardgym.client.StandingBarbellPose;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    // First-person arms are rendered separately from the full player model.
    // Hide both the arm and its sleeve for the entire active exercise.
    @Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("HEAD"), cancellable = true)
    private void hardgym$hideTrainingFirstPersonArms(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                                    int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (BenchClientState.findActiveBench(player) != null
                || DeadliftClientState.findActiveDeadlift(player) != null
                || SquatClientState.findActiveSquat(player) != null) {
            ci.cancel();
        }
    }

    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFF)V",
            at = @At("TAIL"))
    private void hardgym$trainingTransforms(AbstractClientPlayerEntity player, MatrixStack matrices,
                                             float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
        BenchClientState.BenchMatch bench = BenchClientState.findActiveBench(player);
        if (bench != null) {
            float desiredYaw = BenchPressBlock.getFacing(bench.state).asRotation();
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(bodyYaw - desiredYaw));
            matrices.translate(0.0D, 0.10D, 0.02D);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
            matrices.translate(0.0D, -0.02D, 0.0D);
            return;
        }

        DeadliftClientState.DeadliftMatch deadlift = DeadliftClientState.findActiveDeadlift(player);
        if (deadlift != null) {
            float desiredYaw = DeadliftBarBlock.getFacing(deadlift.state).asRotation();
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(bodyYaw - desiredYaw));
            float anim = ExerciseClientState.phase(player.world, deadlift.pos, deadlift.state, tickDelta);
            // Bottom deadlift position: lower the whole rendered body so the feet contact the ground.
            matrices.translate(0.0D, StandingBarbellPose.deadliftBodyOffset(anim), 0.0D);
            return;
        }

        SquatClientState.SquatMatch squat = SquatClientState.findActiveSquat(player);
        if (squat == null) return;
        float desiredYaw = SquatRackBlock.getFacing(squat.state).asRotation() + 180.0F;
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(bodyYaw - desiredYaw));
        float anim = ExerciseClientState.phase(player.world, squat.pos, squat.state, tickDelta);

        // Keep the original whole-player squat position. Foot/ground correction is now
        // applied only to the leg model pivots in BipedEntityModelMixin, so the torso,
        // head, arms and bar stay at their established height.
        matrices.translate(0.0D, StandingBarbellPose.squatBodyOffset(anim), 0.0D);
    }
}
