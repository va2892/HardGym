package com.hardgym.mixin;

import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemFoodFatigueMixin {
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void hardgym$foodRestoresFatigue(ItemStack stack, World world, LivingEntity user,
                                             CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient || !(user instanceof ServerPlayerEntity)) return;

        Item item = (Item)(Object)this;
        if (!item.isFood()) return;
        FoodComponent food = item.getFoodComponent();
        if (food == null) return;

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        GymPlayerData data = (GymPlayerData) player;
        int before = data.hardgym$getFatigue();
        // Food should help recovery, but only a little. Sleep remains the main FAT recovery.
        // Restore roughly half of the food's hunger value, rounded up.
        // Examples: steak (8 hunger) -> 4 FAT, bread (5) -> 3 FAT, apple (4) -> 2 FAT.
        int restore = Math.max(1, (food.getHunger() + 1) / 2);
        data.hardgym$setFatigue(before - restore);
        int restored = before - data.hardgym$getFatigue();

        GymHud.showRecoveryOrHide(player);
        if (restored > 0) {
            player.sendMessage(new LiteralText("Еда: -" + restored + "% FAT")
                    .formatted(Formatting.GREEN), true);
        }
    }
}
