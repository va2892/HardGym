package com.hardgym.mixin.client;

import com.hardgym.block.BenchPressBlock;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.client.BenchClientState;
import com.hardgym.client.DeadliftClientState;
import com.hardgym.client.SquatClientState;
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

    @Inject(method = "update", at = @At("TAIL"))
    private void hardgym$trainingFirstPersonCamera(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                                    boolean inverseView, float tickDelta, CallbackInfo ci) {
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
            int anim = deadlift.state.get(DeadliftBarBlock.ANIM);
            double standOffset = 0.58D;
            double drop = anim == 0 ? 0.12D : (anim == 1 ? 0.06D : 0.0D);
            double camX = deadlift.pos.getX() + 0.5D - facing.getOffsetX() * standOffset;
            double camY = deadlift.pos.getY() + 1.62D - drop;
            double camZ = deadlift.pos.getZ() + 0.5D - facing.getOffsetZ() * standOffset;
            float pitch = anim == 0 ? 24.0F : (anim == 1 ? 12.0F : 0.0F);
            this.setPos(camX, camY, camZ);
            this.setRotation(facing.asRotation(), pitch);
            return;
        }

        SquatClientState.SquatMatch squat = SquatClientState.findActiveSquat(entity);
        if (squat == null) return;
        Direction facing = SquatRackBlock.getFacing(squat.state);
        int anim = squat.state.get(SquatRackBlock.ANIM);
        double drop = anim == 2 ? 0.50D : (anim == 1 ? 0.25D : 0.0D);
        double standOffset = 0.34D;
        double camX = squat.pos.getX() + 0.5D + facing.getOffsetX() * (standOffset + 0.08D);
        double camY = squat.pos.getY() + 1.62D - drop;
        double camZ = squat.pos.getZ() + 0.5D + facing.getOffsetZ() * (standOffset + 0.08D);
        float pitch = anim == 2 ? 8.0F : (anim == 1 ? 4.0F : 0.0F);
        this.setPos(camX, camY, camZ);
        this.setRotation(facing.asRotation() + 180.0F, pitch);
    }
}
