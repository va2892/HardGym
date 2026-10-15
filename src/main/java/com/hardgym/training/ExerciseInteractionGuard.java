package com.hardgym.training;

import com.hardgym.item.DumbbellItem;
import com.hardgym.player.BenchPlayerState;
import com.hardgym.player.DumbbellPlayerState;
import com.hardgym.player.StandingPlayerState;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/** Prevents world actions without consuming the use key that drives barbell reps. */
public final class ExerciseInteractionGuard {
    private ExerciseInteractionGuard() { }

    public static boolean requireEmptyHands(ServerPlayerEntity player) {
        if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()
                && player.getStackInHand(Hand.OFF_HAND).isEmpty()) return true;
        player.sendMessage(new LiteralText("Чтобы начать упражнение со штангой, освободи обе руки.")
                .formatted(Formatting.YELLOW), true);
        return false;
    }

    public static boolean isExercising(PlayerEntity player) {
        if (player == null || !player.isAlive()) return false;

        if (player.world.isClient) {
            // Read only this player's tracked ownership, never nearby stations or
            // the integrated server's static session maps.
            if (player instanceof BenchPlayerState
                    && ((BenchPlayerState) player).hardgym$getBenchPos() != null) return true;
            if (player instanceof StandingPlayerState) {
                StandingPlayerState standing = (StandingPlayerState) player;
                if (standing.hardgym$getSquatPos() != null
                        || standing.hardgym$getDeadliftPos() != null) return true;
            }
        } else if (BenchPressManager.isTraining(player.getUuid())
                || SquatManager.isTraining(player.getUuid())
                || DeadliftManager.isTraining(player.getUuid())) {
            // The server session remains authoritative, including paused and
            // pinned phases and clicks arriving before the next entity sync.
            return true;
        }

        return player instanceof DumbbellPlayerState
                && ((DumbbellPlayerState) player).hardgym$getDumbbellTrainingWeight() > 0
                && player.isUsingItem()
                && player.getActiveItem().getItem() instanceof DumbbellItem;
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                isExercising(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                isExercising(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                isExercising(player) ? TypedActionResult.fail(player.getStackInHand(hand))
                        : TypedActionResult.pass(player.getStackInHand(hand)));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                isExercising(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                isExercising(player) ? ActionResult.FAIL : ActionResult.PASS);
    }
}
