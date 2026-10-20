package com.hardgym.mixin.client;

import com.hardgym.animation.ExerciseAnimation;
import com.hardgym.block.BenchPressBlock;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.client.BenchClientState;
import com.hardgym.client.DeadliftClientState;
import com.hardgym.client.ExerciseClientState;
import com.hardgym.client.SquatClientState;
import com.hardgym.client.StandingBarbellPose;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(D)D"))
    private void hardgym$centerBenchThirdPersonCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!thirdPerson || !(focusedEntity instanceof LivingEntity)) {
            return;
        }

        BenchClientState.BenchMatch bench = BenchClientState.findActiveBench((LivingEntity) focusedEntity);

        if (bench == null) {
            return;
        }

        this.setPos(bench.pos.getX() + 0.5D, bench.pos.getY() + 0.65D, bench.pos.getZ() + 0.5D);
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void hardgym$trainingFirstPersonCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (thirdPerson || inverseView || !(focusedEntity instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) focusedEntity;

        BenchClientState.BenchMatch bench = BenchClientState.findActiveBench(entity);
        if (bench != null) {
            Direction rackDirection = BenchPressBlock.getFacing(bench.state).getOpposite();
            double camX = bench.pos.getX() + 0.5D + rackDirection.getOffsetX() * 0.60D;
            double camY = bench.pos.getY() + 0.74D;
            double camZ = bench.pos.getZ() + 0.5D + rackDirection.getOffsetZ() * 0.60D;
            double targetX = bench.pos.getX() + 0.5D + rackDirection.getOffsetX() * 0.18D;
            double targetY = bench.pos.getY() + 1.02D;
            double targetZ = bench.pos.getZ() + 0.5D + rackDirection.getOffsetZ() * 0.18D;
            double dx = targetX - camX, dy = targetY - camY, dz = targetZ - camZ;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
            float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
            this.setPos(camX, camY, camZ);
            this.setRotation(yaw, pitch);
            return;
        }

        DeadliftClientState.DeadliftMatch deadlift = DeadliftClientState.findActiveDeadlift(entity);
        if (deadlift != null) {
            Direction facing = DeadliftBarBlock.getFacing(deadlift.state);
            float anim = ExerciseClientState.phase(entity.world, deadlift.pos, deadlift.state, tickDelta);
            double standOffset = 0.58D;
            double drop = ExerciseAnimation.pose(anim, 0.12F, 0.06F, 0.0F);
            double camX = deadlift.pos.getX() + 0.5D - facing.getOffsetX() * standOffset;
            double camY = deadlift.pos.getY() + 1.62D - drop;
            double camZ = deadlift.pos.getZ() + 0.5D - facing.getOffsetZ() * standOffset;
            float pitch = ExerciseAnimation.pose(anim, 24.0F, 12.0F, 0.0F);
            this.setPos(camX, camY, camZ);
            this.setRotation(facing.asRotation(), pitch);
            return;
        }

        SquatClientState.SquatMatch squat = SquatClientState.findActiveSquat(entity);
        if (squat == null) return;
        Direction facing = SquatRackBlock.getFacing(squat.state);
        float anim = ExerciseClientState.phase(entity.world, squat.pos, squat.state, tickDelta);
        double drop = ExerciseAnimation.pose(anim, 0.0F, 0.25F, 0.50F);
        double standOffset = 0.34D;
        double firstPersonCameraOffset = standOffset - 0.20D + StandingBarbellPose.PLAYER_SCALE * (StandingBarbellPose.squatUpperBodyZ(anim)
            - StandingBarbellPose.squatUpperBodyZ(0.0F)) / 16.0D;
        double camX = squat.pos.getX() + 0.5D + facing.getOffsetX() * firstPersonCameraOffset;
        double camY = squat.pos.getY() + 1.62D - drop;
        double camZ = squat.pos.getZ() + 0.5D + facing.getOffsetZ() * firstPersonCameraOffset;
        float pitch = ExerciseAnimation.pose(anim, 0.0F, 4.0F, 8.0F);
        this.setPos(camX, camY, camZ);
        this.setRotation(facing.asRotation() + 180.0F, pitch);
    }
}
