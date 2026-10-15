package com.hardgym.mixin;

import com.hardgym.training.ExerciseInteractionGuard;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ExerciseInventoryNetworkMixin {
    @Shadow public ServerPlayerEntity player;

    @Unique
    private boolean hardgym$handsLocked(Packet<ServerPlayPacketListener> packet) {
        // These injections run at HEAD, before vanilla's own thread check.
        // Reschedule first so session maps and inventory are read only on the server thread.
        NetworkThreadUtils.forceMainThread(packet, (ServerPlayNetworkHandler) (Object) this,
                player.getServerWorld());
        return ExerciseInteractionGuard.isExercising(player);
    }

    @Unique
    private void hardgym$refreshInventory() {
        player.refreshScreenHandler(player.currentScreenHandler);
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void hardgym$keepSelectedSlot(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        if (hardgym$handsLocked(packet)) {
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.inventory.selectedSlot));
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void hardgym$keepHands(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (!hardgym$handsLocked(packet)) return;
        switch (packet.getAction()) {
            case SWAP_ITEM_WITH_OFFHAND:
            case DROP_ITEM:
            case DROP_ALL_ITEMS:
                hardgym$refreshInventory();
                ci.cancel();
                break;
            default:
                // RELEASE_USE_ITEM must reach vanilla to end dumbbell training.
                break;
        }
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventSlotClick(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (hardgym$handsLocked(packet)) {
            // Acknowledge receipt, then replace client prediction with the unchanged
            // inventory. No pending transaction may survive the end of the workout.
            player.networkHandler.sendPacket(new ConfirmScreenActionS2CPacket(
                    packet.getSyncId(), packet.getActionId(), true));
            hardgym$refreshInventory();
            ci.cancel();
        }
    }

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventCreativeMove(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        if (hardgym$handsLocked(packet)) {
            hardgym$refreshInventory();
            ci.cancel();
        }
    }

    @Inject(method = "onPickFromInventory", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventInventoryPick(PickFromInventoryC2SPacket packet, CallbackInfo ci) {
        if (hardgym$handsLocked(packet)) {
            hardgym$refreshInventory();
            ci.cancel();
        }
    }

    @Inject(method = "onCraftRequest", at = @At("HEAD"), cancellable = true)
    private void hardgym$preventRecipeMove(CraftRequestC2SPacket packet, CallbackInfo ci) {
        if (hardgym$handsLocked(packet)) {
            hardgym$refreshInventory();
            ci.cancel();
        }
    }
}
