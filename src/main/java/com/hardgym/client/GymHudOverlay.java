package com.hardgym.client;

import com.hardgym.player.GymHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/** Compact HardGym workout HUD rendered above the vanilla hotbar. */
public final class GymHudOverlay {
    private static int mode = 0;
    private static int weightKg = 0;
    private static int reps = 0;
    private static int stamina = 100;
    private static int fatigue = 0;
    private static int progress = 0;
    private static int health = 20;
    private static int maxUnlockedKg = 0;
    private static int ticksSinceUpdate = 9999;

    private GymHudOverlay() { }

    public static void update(int newMode, int newWeightKg, int newReps, int newStamina, int newFatigue,
                              int newProgress, int newHealth, int newMaxUnlockedKg) {
        mode = newMode;
        weightKg = newWeightKg;
        reps = newReps;
        stamina = clamp(newStamina);
        fatigue = clamp(newFatigue);
        progress = clamp(newProgress);
        health = Math.max(0, newHealth);
        maxUnlockedKg = Math.max(0, newMaxUnlockedKg);
        ticksSinceUpdate = 0;
    }

    public static void tick() { ticksSinceUpdate++; }

    public static void render(MatrixStack matrices) {
        if (mode == GymHud.MODE_HIDDEN) return;
        if (mode != GymHud.MODE_RECOVERY && ticksSinceUpdate > 18 && stamina >= 100) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        TextRenderer text = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int panelWidth = 238;
        int panelHeight = 58;
        int x = (screenWidth - panelWidth) / 2;
        int y = screenHeight - 132;

        DrawableHelper.fill(matrices, x, y, x + panelWidth, y + panelHeight, 0xB815171B);
        DrawableHelper.fill(matrices, x, y, x + panelWidth, y + 1, 0xCC5A5F68);
        DrawableHelper.fill(matrices, x, y + panelHeight - 1, x + panelWidth, y + panelHeight, 0xCC050607);
        DrawableHelper.fill(matrices, x, y, x + 1, y + panelHeight, 0xCC5A5F68);
        DrawableHelper.fill(matrices, x + panelWidth - 1, y, x + panelWidth, y + panelHeight, 0xCC050607);

        boolean squatPinned = mode == GymHud.MODE_SQUAT_PINNED;
        boolean pinned = mode == GymHud.MODE_PINNED || squatPinned;
        boolean recovering = mode == GymHud.MODE_RECOVERY;
        boolean squat = mode == GymHud.MODE_SQUAT || mode == GymHud.MODE_SQUAT_WAITING || squatPinned;
        boolean deadlift = mode == GymHud.MODE_DEADLIFT || mode == GymHud.MODE_DEADLIFT_WAITING;
        int accent = accentColor(weightKg, pinned, recovering);
        int titleColor = pinned ? 0xFFFF7777 : 0xFFF4F4F4;
        DrawableHelper.fill(matrices, x + 1, y + 1, x + 5, y + panelHeight - 1, accent);

        String title;
        if (mode == GymHud.MODE_DUMBBELL) title = "ГАНТЕЛЬ  " + weightKg + " КГ";
        else if (recovering) title = "ВОССТАНОВЛЕНИЕ";
        else if (squat) title = "ПРИСЕД  " + weightKg + " КГ";
        else if (deadlift) title = "СТАНОВАЯ  " + weightKg + " КГ";
        else if (pinned) title = "ПРИДАВИЛО  " + weightKg + " КГ";
        else title = "ЖИМ ЛЁЖА  " + weightKg + " КГ";

        String repText = recovering ? "" : "ПОВТ  " + reps;
        String state = stateText();
        text.drawWithShadow(matrices, title, x + 11, y + 7, titleColor);
        if (!pinned) {
            text.drawWithShadow(matrices, state, x + panelWidth / 2 - text.getWidth(state) / 2, y + 7, 0xFFB8C0C8);
        }
        if (!repText.isEmpty()) {
            text.drawWithShadow(matrices, repText, x + panelWidth - 10 - text.getWidth(repText), y + 7, 0xFFE5E5E5);
        }

        if (pinned) renderPinned(matrices, text, x, y, panelWidth);
        else renderNormal(matrices, text, x, y, panelWidth, recovering);
    }

    private static void renderNormal(MatrixStack matrices, TextRenderer text, int x, int y, int panelWidth, boolean recovering) {
        int railX = x + 11;
        int railY = y + 22;
        int railW = panelWidth - 22;
        int railH = 5;
        int railColor = recovering ? 0xFF55DD6A : 0xFF33C7FF;
        drawBar(matrices, railX, railY, railW, railH, progress, railColor, 0xFF2A3036);

        int barY = y + 43;
        int labelX1 = x + 11;
        int labelX2 = x + 123;
        int barW = 99;
        String stLabel = "ST " + stamina + "%";
        String fatLabel = "FAT " + fatigue + "%";
        text.drawWithShadow(matrices, stLabel, labelX1, barY - 9, staminaColor(stamina));
        text.drawWithShadow(matrices, fatLabel, labelX2, barY - 9, fatigueColor(fatigue));
        drawBar(matrices, labelX1, barY, barW, 5, stamina, staminaBarColor(stamina), 0xFF252A2F);
        drawBar(matrices, labelX2, barY, barW, 5, fatigue, fatigueBarColor(fatigue), 0xFF252A2F);
    }

    private static void renderPinned(MatrixStack matrices, TextRenderer text, int x, int y, int panelWidth) {
        String warning = "SHIFT — СБРОСИТЬ ШТАНГУ";
        text.drawWithShadow(matrices, warning, x + panelWidth / 2 - text.getWidth(warning) / 2, y + 23, 0xFFFF5555);
        int barY = y + 43;
        int leftX = x + 11;
        int rightX = x + 123;
        int barW = 99;
        int healthPercent = clamp(Math.round((health / 20.0F) * 100.0F));
        String hpLabel = "HP " + health;
        String fatLabel = "FAT " + fatigue + "%";
        text.drawWithShadow(matrices, hpLabel, leftX, barY - 9, 0xFFFF7777);
        text.drawWithShadow(matrices, fatLabel, rightX, barY - 9, fatigueColor(fatigue));
        drawBar(matrices, leftX, barY, barW, 5, healthPercent, 0xFFFF4B4B, 0xFF252A2F);
        drawBar(matrices, rightX, barY, barW, 5, fatigue, fatigueBarColor(fatigue), 0xFF252A2F);
    }

    private static void drawBar(MatrixStack matrices, int x, int y, int width, int height,
                                int percent, int fillColor, int backgroundColor) {
        DrawableHelper.fill(matrices, x, y, x + width, y + height, backgroundColor);
        int inner = Math.round((width - 2) * (clamp(percent) / 100.0F));
        if (inner > 0) DrawableHelper.fill(matrices, x + 1, y + 1, x + 1 + inner, y + height - 1, fillColor);
    }

    private static int staminaColor(int value) {
        if (value <= 20) return 0xFFFF5555;
        if (value <= 45) return 0xFFFFC857;
        return 0xFF74E07A;
    }
    private static int staminaBarColor(int value) {
        if (value <= 20) return 0xFFE74747;
        if (value <= 45) return 0xFFE5A72B;
        return 0xFF4BC263;
    }
    private static int fatigueColor(int value) {
        if (value >= 80) return 0xFFFF5555;
        if (value >= 50) return 0xFFFFA94D;
        return 0xFFD4D4D4;
    }
    private static int fatigueBarColor(int value) {
        if (value >= 80) return 0xFFE74747;
        if (value >= 50) return 0xFFE58C35;
        return 0xFF87919B;
    }

    private static String stateText() {
        if (mode == GymHud.MODE_BENCH_WAITING || mode == GymHud.MODE_SQUAT_WAITING
                || mode == GymHud.MODE_DEADLIFT_WAITING) return "ДЕРЖИ ПКМ";
        if (mode == GymHud.MODE_BENCH) return "ЖИМ";
        if (mode == GymHud.MODE_SQUAT) return "ПРИСЕД";
        if (mode == GymHud.MODE_DEADLIFT) return "ТЯНИ";
        if (mode == GymHud.MODE_DUMBBELL) return "ПОДХОД";
        if (mode == GymHud.MODE_RECOVERY) return "ОТДЫХ";
        return "";
    }

    private static int accentColor(int weight, boolean pinned, boolean recovering) {
        if (pinned) return 0xFFFF5555;
        if (recovering) return staminaBarColor(stamina);
        if (mode == GymHud.MODE_BENCH || mode == GymHud.MODE_BENCH_WAITING
                || mode == GymHud.MODE_SQUAT || mode == GymHud.MODE_SQUAT_WAITING
                || mode == GymHud.MODE_DEADLIFT || mode == GymHud.MODE_DEADLIFT_WAITING) {
            return relativeWeightColor(weight, maxUnlockedKg);
        }
        return 0xFFFFC857;
    }

    private static int relativeWeightColor(int weight, int maxWeight) {
        if (maxWeight <= 0 || weight <= 0) return 0xFFFFC857;
        float ratio = weight / (float) maxWeight;
        if (ratio <= 0.50F) return 0xFF55DD6A;
        if (ratio <= 0.75F) return 0xFFC9D94B;
        if (ratio <= 0.90F) return 0xFFFFB347;
        return 0xFFFF5A55;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
