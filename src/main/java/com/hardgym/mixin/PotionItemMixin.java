package com.hardgym.mixin;

import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionItem.class)
public abstract class PotionItemMixin {
    private static final int HARDGYM_WATER_STAMINA_RESTORE = 25;

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void hardgym$restoreStaminaFromWater(ItemStack stack, World world, LivingEntity user,
                                                  CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient || !(user instanceof ServerPlayerEntity)) {
            return;
        }

        if (PotionUtil.getPotion(stack) != Potions.WATER) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        GymPlayerData data = (GymPlayerData) player;
        int before = data.hardgym$getStamina();
        int after = Math.min(100, before + HARDGYM_WATER_STAMINA_RESTORE);
        int restored = after - before;

        data.hardgym$setStamina(after);
        GymHud.showRecoveryOrHide(player);

        if (restored > 0) {
            player.sendMessage(new LiteralText("Вода: +" + restored + "% stamina")
                    .formatted(Formatting.AQUA), true);
        } else {
            player.sendMessage(new LiteralText("Stamina уже полная")
                    .formatted(Formatting.GRAY), true);
        }
    }
}
