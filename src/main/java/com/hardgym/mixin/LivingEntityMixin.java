package com.hardgym.mixin;

import com.hardgym.player.BenchPlayerState;
import com.hardgym.player.GymPhysicalStats;
import com.hardgym.player.GymPhysiqueClientState;
import com.hardgym.player.GymPlayerData;
import com.hardgym.player.GymStats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void hardgym$benchReplacesLocomotion(Vec3d movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof BenchPlayerState
                && ((BenchPlayerState)self).hardgym$getBenchPos() != null) {
            // Skip locomotion and gravity during the set, without changing the
            // NoGravity value that vanilla writes into the player's save data.
            self.setVelocity(0.0D, 0.0D, 0.0D);
            self.fallDistance = 0.0F;
            self.setOnGround(true);
            ci.cancel();
        }
    }

    @Inject(method = "getJumpVelocity", at = @At("RETURN"), cancellable = true)
    private void hardgym$modifyPlayerJumpVelocity(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) self;
        int deadliftLevel;
        if (player.world.isClient) {
            deadliftLevel = GymPhysiqueClientState.getDeadliftLevel();
        } else {
            deadliftLevel = GymStats.levelForXp(((GymPlayerData)player).hardgym$getDeadliftXp());
        }

        // Preserve Jump Boost / other additions by replacing only vanilla's 0.42 base component.
        float vanillaResult = cir.getReturnValue();
        float wantedBase = GymPhysicalStats.jumpVelocityForDeadliftLevel(deadliftLevel);
        cir.setReturnValue(vanillaResult + (wantedBase - 0.42F));
    }
}
