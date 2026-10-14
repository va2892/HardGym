package com.hardgym.mixin;

import com.hardgym.player.BenchPlayerState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class BenchPlayerPhysicsMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void hardgym$keepBenchPlayerStill(MovementType type, Vec3d movement, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof BenchPlayerState)
                || ((BenchPlayerState)self).hardgym$getBenchPos() == null) {
            return;
        }

        // Run on both sides: client prediction must not fight the server's anchor.
        // Input, gravity, knockback and collision responses cannot move the lifter.
        self.setVelocity(0.0D, 0.0D, 0.0D);
        self.fallDistance = 0.0F;
        self.setOnGround(true);
        ci.cancel();
    }
}
