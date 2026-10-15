package com.hardgym.mixin;

import com.hardgym.training.ExerciseInteractionGuard;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventExerciseBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // AttackBlockCallback handles the initial hit. This also catches a block
        // break started before the set and completed later by a queued packet.
        if (ExerciseInteractionGuard.isExercising(player)) cir.setReturnValue(false);
    }
}
