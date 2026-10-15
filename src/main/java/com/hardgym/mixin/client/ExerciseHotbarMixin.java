package com.hardgym.mixin.client;

import com.hardgym.training.ExerciseInteractionGuard;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class ExerciseHotbarMixin {
    @Shadow @Final public PlayerEntity player;

    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventWorkoutScroll(double amount, CallbackInfo ci) {
        if (ExerciseInteractionGuard.isExercising(player)) ci.cancel();
    }
}
