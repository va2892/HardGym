package com.hardgym.mixin.client;

import com.hardgym.client.DumbbellArmPose;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;

    @Shadow
    private void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                        Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                        MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        throw new AssertionError();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"), cancellable = true)
    private void hardgym$pairedCurl(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate consumers,
                                    ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (!DumbbellArmPose.isCurling(player)) return;
        ItemStack pair = DumbbellArmPose.heldPair(player);
        if (pair.isEmpty()) return;
        boolean main = player.getActiveHand() == Hand.MAIN_HAND;
        float equip = 1.0F - MathHelper.lerp(tickDelta,
                main ? prevEquipProgressMainHand : prevEquipProgressOffHand,
                main ? equipProgressMainHand : equipProgressOffHand);
        float lift = DumbbellArmPose.lift(player, tickDelta);
        matrices.push();
        try {
            // Both hands keep their own horizontal offset and curl toward their
            // own shoulder with the same eased timer, never the bow-use pose.
            matrices.translate(0.0D, 0.16D * lift, 0.12D * lift);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-18.0F * lift));
            float pitch = player.getPitch(tickDelta);
            renderFirstPersonItem(player, tickDelta, pitch, Hand.MAIN_HAND, 0.0F, pair, equip,
                    matrices, consumers, light);
            renderFirstPersonItem(player, tickDelta, pitch, Hand.OFF_HAND, 0.0F, pair, equip,
                    matrices, consumers, light);
        } finally {
            matrices.pop();
        }
        ci.cancel();
    }
}
