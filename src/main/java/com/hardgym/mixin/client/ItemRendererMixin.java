package com.hardgym.mixin.client;

import com.hardgym.item.DumbbellPairItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Two dumbbells in the icon, one model per hand or physical rack position. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Unique private boolean hardgym$renderingPairIcon;

    @Shadow
    public abstract void renderItem(ItemStack stack, ModelTransformation.Mode mode, boolean leftHanded,
                                    MatrixStack matrices, VertexConsumerProvider consumers,
                                    int light, int overlay, BakedModel model);

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"), cancellable = true)
    private void hardgym$pairIcon(ItemStack stack, ModelTransformation.Mode mode, boolean leftHanded,
                                MatrixStack matrices, VertexConsumerProvider consumers, int light,
                                int overlay, BakedModel model, CallbackInfo ci) {
        if (hardgym$renderingPairIcon || mode != ModelTransformation.Mode.GUI
                || !(stack.getItem() instanceof DumbbellPairItem)) return;
        hardgym$renderingPairIcon = true;
        try {
            for (int index = 0; index < 2; index++) {
                matrices.push();
                try {
                    matrices.translate(index == 0 ? -0.16D : 0.16D, index == 0 ? 0.20D : -0.20D, 0.0D);
                    matrices.scale(0.65F, 0.65F, 0.65F);
                    renderItem(stack, mode, leftHanded, matrices, consumers, light, overlay, model);
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            hardgym$renderingPairIcon = false;
        }
        ci.cancel();
    }
}
