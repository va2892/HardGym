package com.hardgym.mixin;

import com.hardgym.training.ExerciseInteractionGuard;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ExerciseItemPickupMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventWorkoutPickup(PlayerEntity player, CallbackInfo ci) {
        // Leave the entity and its stack untouched; it can be picked up after the set.
        if (ExerciseInteractionGuard.isExercising(player)) ci.cancel();
    }
}
