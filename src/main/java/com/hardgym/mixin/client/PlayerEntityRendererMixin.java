package com.hardgym.mixin.client;

import com.hardgym.block.BenchPressBlock;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.client.BenchClientState;
import com.hardgym.client.DeadliftClientState;
import com.hardgym.client.SquatClientState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

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
            int anim = deadlift.state.get(DeadliftBarBlock.ANIM);
            // Bottom deadlift position: lower the whole rendered body so the feet contact the ground.
            if (anim == 0) matrices.translate(0.0D, -0.28D, 0.0D);
            else if (anim == 1) matrices.translate(0.0D, -0.06D, 0.0D);
            return;
        }

        SquatClientState.SquatMatch squat = SquatClientState.findActiveSquat(player);
        if (squat == null) return;
        float desiredYaw = SquatRackBlock.getFacing(squat.state).asRotation() + 180.0F;
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(bodyYaw - desiredYaw));
        int anim = squat.state.get(SquatRackBlock.ANIM);
        if (anim == 2) matrices.translate(0.0D, -0.50D, 0.0D);
        else if (anim == 1) matrices.translate(0.0D, -0.25D, 0.0D);
    }
}
