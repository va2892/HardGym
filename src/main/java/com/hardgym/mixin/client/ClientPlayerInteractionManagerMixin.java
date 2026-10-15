package com.hardgym.mixin.client;

import com.hardgym.training.ExerciseInteractionGuard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventWorkoutSlotClick(int syncId, int slotId, int clickData,
                                               SlotActionType actionType, PlayerEntity player,
                                               CallbackInfoReturnable<ItemStack> cir) {
        if (ExerciseInteractionGuard.isExercising(player)) cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void hardgym$stopExerciseMining(BlockPos pos, Direction direction,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (ExerciseInteractionGuard.isExercising(MinecraftClient.getInstance().player)) {
            ((ClientPlayerInteractionManager) (Object) this).cancelBlockBreaking();
            cir.setReturnValue(false);
        }
    }
}
