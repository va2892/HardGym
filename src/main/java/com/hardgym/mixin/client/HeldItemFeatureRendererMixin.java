package com.hardgym.mixin.client;

import com.hardgym.client.DumbbellArmPose;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use the native hand attachment, including BendyLib's bent-forearm transform. */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
    @Shadow
    private void renderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode mode,
                            Arm arm, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        throw new AssertionError();
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void hardgym$secondDumbbell(LivingEntity entity, ItemStack stack, ModelTransformation.Mode mode,
                                        Arm arm, MatrixStack matrices, VertexConsumerProvider consumers,
                                        int light, CallbackInfo ci) {
        if (!stack.isEmpty()) return;
        ItemStack pair = DumbbellArmPose.heldPair(entity);
        if (pair.isEmpty()) return;
        // Native render() calls this for both arms. Supply the same ONE inventory
        // item for the empty hand; the recursive call has a nonempty stack.
        renderItem(entity, pair, mode, arm, matrices, consumers, light);
        ci.cancel();
    }
}
