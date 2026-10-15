package com.hardgym.mixin.client;

import com.hardgym.training.ExerciseInteractionGuard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class ExerciseInputMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void hardgym$keepWorkoutHands(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!ExerciseInteractionGuard.isExercising(client.player)) return;
        for (KeyBinding key : client.options.keysHotbar) {
            while (key.wasPressed()) { }
        }
        while (client.options.keySwapHands.wasPressed()) { }
        while (client.options.keyDrop.wasPressed()) { }
        while (client.options.keyInventory.wasPressed()) { }
        // Do not consume keyUse, keySneak or perspective/look controls.
    }

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventWorkoutPick(CallbackInfo ci) {
        if (ExerciseInteractionGuard.isExercising(((MinecraftClient) (Object) this).player)) ci.cancel();
    }
}
