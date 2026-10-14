package com.hardgym.player;

import com.hardgym.HardGymMod;
import com.hardgym.training.BenchPressManager;
import com.hardgym.training.SquatManager;
import com.hardgym.training.DeadliftManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/** Server-side HUD synchronizer. */
public final class GymHud {
    public static final int MODE_HIDDEN = 0;
    public static final int MODE_DUMBBELL = 1;
    public static final int MODE_BENCH = 2;
    public static final int MODE_BENCH_WAITING = 3;
    public static final int MODE_PINNED = 4;
    public static final int MODE_RECOVERY = 5;
    public static final int MODE_SQUAT = 6;
    public static final int MODE_SQUAT_WAITING = 7;
    public static final int MODE_SQUAT_PINNED = 8;
    public static final int MODE_DEADLIFT = 9;
    public static final int MODE_DEADLIFT_WAITING = 10;
    public static final int MODE_DUMBBELL_PAIR = 11;

    private GymHud() { }

    public static void showDumbbell(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue) {
        showDumbbell(player, weightKg, false, rep, stamina, fatigue);
    }

    public static void showDumbbell(ServerPlayerEntity player, int weightKg, boolean pair,
                                    int rep, int stamina, int fatigue) {
        send(player, pair ? MODE_DUMBBELL_PAIR : MODE_DUMBBELL, weightKg, rep, stamina, fatigue,
                100, Math.round(player.getHealth()));
    }

    public static void showBench(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue, int progressPercent) {
        send(player, MODE_BENCH, weightKg, rep, stamina, fatigue, progressPercent, Math.round(player.getHealth()));
    }

    public static void showBenchWaiting(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue, int progressPercent) {
        send(player, MODE_BENCH_WAITING, weightKg, rep, stamina, fatigue, progressPercent, Math.round(player.getHealth()));
    }

    public static void showBenchPinned(ServerPlayerEntity player, int weightKg, int rep, int fatigue, int health) {
        send(player, MODE_PINNED, weightKg, rep, 0, fatigue, 100, health);
    }

    public static void showSquat(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue, int progressPercent) {
        send(player, MODE_SQUAT, weightKg, rep, stamina, fatigue, progressPercent, Math.round(player.getHealth()));
    }

    public static void showSquatWaiting(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue, int progressPercent) {
        send(player, MODE_SQUAT_WAITING, weightKg, rep, stamina, fatigue, progressPercent, Math.round(player.getHealth()));
    }

    public static void showSquatPinned(ServerPlayerEntity player, int weightKg, int rep, int fatigue, int health) {
        send(player, MODE_SQUAT_PINNED, weightKg, rep, 0, fatigue, 100, health);
    }


    public static void showDeadlift(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue, int progressPercent) {
        send(player, MODE_DEADLIFT, weightKg, rep, stamina, fatigue, progressPercent, Math.round(player.getHealth()));
    }

    public static void showDeadliftWaiting(ServerPlayerEntity player, int weightKg, int rep, int stamina, int fatigue, int progressPercent) {
        send(player, MODE_DEADLIFT_WAITING, weightKg, rep, stamina, fatigue, progressPercent, Math.round(player.getHealth()));
    }

    public static void showRecovery(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        int stamina = data.hardgym$getStamina();
        int fatigue = data.hardgym$getFatigue();
        send(player, MODE_RECOVERY, 0, 0, stamina, fatigue, stamina, Math.round(player.getHealth()));
    }

    public static void showRecoveryOrHide(ServerPlayerEntity player) {
        if (player == null) return;
        GymPlayerData data = (GymPlayerData) player;
        if (data.hardgym$getStamina() < 100) showRecovery(player); else hide(player);
    }

    public static void hide(ServerPlayerEntity player) {
        if (player == null) return;
        send(player, MODE_HIDDEN, 0, 0, 100, 0, 0, Math.round(player.getHealth()));
    }

    private static void send(ServerPlayerEntity player, int mode, int weightKg, int rep, int stamina,
                             int fatigue, int progressPercent, int health) {
        if (player == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(mode);
        buf.writeInt(weightKg);
        buf.writeInt(rep);
        buf.writeInt(GymStats.clamp(stamina, 0, 100));
        buf.writeInt(GymStats.clamp(fatigue, 0, 100));
        buf.writeInt(GymStats.clamp(progressPercent, 0, 100));
        buf.writeInt(Math.max(0, health));
        buf.writeInt(maxUnlockedWeightForMode(player, mode));
        ServerPlayNetworking.send(player, HardGymMod.GYM_HUD_PACKET, buf);
    }

    private static int maxUnlockedWeightForMode(ServerPlayerEntity player, int mode) {
        GymPlayerData data = (GymPlayerData) player;
        if (mode == MODE_SQUAT || mode == MODE_SQUAT_WAITING || mode == MODE_SQUAT_PINNED) {
            return SquatManager.maxUnlockedWeight(data);
        }
        if (mode == MODE_DEADLIFT || mode == MODE_DEADLIFT_WAITING) {
            return DeadliftManager.maxUnlockedWeight(data);
        }
        return BenchPressManager.maxUnlockedWeight(data);
    }
}
