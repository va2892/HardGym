package com.hardgym.player;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

/**
 * Strength detraining system.
 * Time is counted only while the player is online in the world.
 */
public final class StrengthDecay {
    // Minecraft day = 20 minutes = 1200 seconds.
    public static final int GRACE_SECONDS = 3 * 1200;
    public static final int DECAY_INTERVAL_SECONDS = 1200;
    private static final double DECAY_FRACTION = 0.02D;
    private static final int MIN_XP_LOSS = 10;

    private StrengthDecay() {
    }

    public static void markTraining(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        data.hardgym$setStrengthIdleSeconds(0);
    }

    public static void tickOneSecond(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        int previousSeconds = data.hardgym$getStrengthIdleSeconds();
        int currentSeconds = previousSeconds + 1;
        data.hardgym$setStrengthIdleSeconds(currentSeconds);

        if (data.hardgym$getStrengthXp() <= 0 || currentSeconds <= GRACE_SECONDS) {
            return;
        }

        int sinceGrace = currentSeconds - GRACE_SECONDS;
        if (sinceGrace % DECAY_INTERVAL_SECONDS != 0) {
            return;
        }

        int oldXp = data.hardgym$getStrengthXp();
        int oldLevel = GymStats.levelForXp(oldXp);
        int loss = Math.max(MIN_XP_LOSS, (int) Math.ceil(oldXp * DECAY_FRACTION));
        int newXp = Math.max(0, oldXp - loss);
        data.hardgym$setStrengthXp(newXp);

        int newLevel = GymStats.levelForXp(newXp);
        if (newLevel < oldLevel) {
            player.sendMessage(new LiteralText(
                    "HardGym: из-за долгого перерыва Strength снизился до " + newLevel + "."
            ).formatted(Formatting.RED), false);
        } else {
            player.sendMessage(new LiteralText(
                    "HardGym: без тренировок сила понемногу падает (-" + (oldXp - newXp) + " Strength XP)."
            ).formatted(Formatting.GRAY), false);
        }

        GymAttributeManager.update(player);
    }

    public static String statusText(GymPlayerData data) {
        int idle = data.hardgym$getStrengthIdleSeconds();
        if (idle < GRACE_SECONDS) {
            int left = GRACE_SECONDS - idle;
            return "Детренировка через: " + formatDuration(left);
        }
        int intoDecay = idle - GRACE_SECONDS;
        int untilNext = DECAY_INTERVAL_SECONDS - (intoDecay % DECAY_INTERVAL_SECONDS);
        if (untilNext == DECAY_INTERVAL_SECONDS && intoDecay > 0) {
            untilNext = DECAY_INTERVAL_SECONDS;
        }
        return "Детренировка активна, следующая потеря через: " + formatDuration(untilNext);
    }

    private static String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}
