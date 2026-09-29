package com.hardgym;

import com.hardgym.block.BenchPressBlock;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.item.DumbbellItem;
import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
import com.hardgym.player.GymStats;
import com.hardgym.player.GymAttributeManager;
import com.hardgym.player.GymPhysicalStats;
import com.hardgym.player.StrengthDecay;
import com.hardgym.player.StrengthRepTable;
import com.hardgym.training.BenchPressManager;
import com.hardgym.training.DeadliftManager;
import com.hardgym.training.SquatManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class HardGymMod implements ModInitializer {
    public static final String MOD_ID = "hardgym";
    public static final Identifier BENCH_INPUT_PACKET = new Identifier(MOD_ID, "bench_press_input");
    public static final Identifier SQUAT_INPUT_PACKET = new Identifier(MOD_ID, "squat_input");
    public static final Identifier DEADLIFT_INPUT_PACKET = new Identifier(MOD_ID, "deadlift_input");
    public static final Identifier DEADLIFT_STATE_PACKET = new Identifier(MOD_ID, "deadlift_state");
    public static final Identifier GYM_HUD_PACKET = new Identifier(MOD_ID, "gym_hud");
    public static final Identifier PHYSIQUE_SYNC_PACKET = new Identifier(MOD_ID, "physique_sync");

    public static final Item DUMBBELL_5KG = new DumbbellItem(5, 1, 4, 6, 5, 3, 16,
            new Item.Settings().group(ItemGroup.MISC).maxCount(1));
    public static final Item DUMBBELL_10KG = new DumbbellItem(10, 2, 8, 10, 8, 5, 20,
            new Item.Settings().group(ItemGroup.MISC).maxCount(1));
    public static final Item DUMBBELL_20KG = new DumbbellItem(20, 5, 16, 18, 14, 9, 28,
            new Item.Settings().group(ItemGroup.MISC).maxCount(1));

    public static final Block BENCH_PRESS_STATION = new BenchPressBlock(
            AbstractBlock.Settings.of(Material.METAL).strength(3.0F, 6.0F).sounds(BlockSoundGroup.METAL).nonOpaque());
    public static final Block SQUAT_RACK = new SquatRackBlock(
            AbstractBlock.Settings.of(Material.METAL).strength(3.5F, 7.0F).sounds(BlockSoundGroup.METAL).nonOpaque());
    public static final Block DEADLIFT_BAR = new DeadliftBarBlock(
            AbstractBlock.Settings.of(Material.METAL).strength(3.0F, 6.0F).sounds(BlockSoundGroup.METAL).nonOpaque());

    private int recoveryTicker = 0;
    private int fatigueRecoverySeconds = 0;
    private final Map<UUID, Integer> sleepTicks = new HashMap<>();
    private final Map<UUID, Long> sleepStartTime = new HashMap<>();
    private static final int FATIGUE_PASSIVE_INTERVAL_SECONDS = 10;
    private static final int SLEEP_FATIGUE_RESTORE = 60;

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "dumbbell_5kg"), DUMBBELL_5KG);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "dumbbell_10kg"), DUMBBELL_10KG);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "dumbbell_20kg"), DUMBBELL_20KG);
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "bench_press_station"), BENCH_PRESS_STATION);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "bench_press_station"),
                new BlockItem(BENCH_PRESS_STATION, new Item.Settings().group(ItemGroup.DECORATIONS)));
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "squat_rack"), SQUAT_RACK);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "squat_rack"),
                new BlockItem(SQUAT_RACK, new Item.Settings().group(ItemGroup.DECORATIONS)));
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "deadlift_bar"), DEADLIFT_BAR);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "deadlift_bar"),
                new BlockItem(DEADLIFT_BAR, new Item.Settings().group(ItemGroup.DECORATIONS)));

        registerCommands();
        registerInputNetworking();
        registerServerTick();
        registerRespawnCopy();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("gymstats").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                GymPlayerData data = (GymPlayerData) player;
                int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
                int bicepsLevel = GymStats.levelForXp(data.hardgym$getBicepsXp());
                int chestLevel = GymStats.levelForXp(data.hardgym$getChestXp());
                int tricepsLevel = GymStats.levelForXp(data.hardgym$getTricepsXp());
                int legsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
                int benchLevel = GymStats.levelForXp(data.hardgym$getBenchXp());
                int squatLevel = GymStats.levelForXp(data.hardgym$getSquatXp());
                int deadliftLevel = GymStats.levelForXp(data.hardgym$getDeadliftXp());

                player.sendMessage(new LiteralText("========== HARDGYM ==========").formatted(Formatting.GOLD), false);
                player.sendMessage(new LiteralText("Strength: Lv." + strengthLevel + "  (" + data.hardgym$getStrengthXp() + " XP, "
                        + GymStats.xpToNextLevel(data.hardgym$getStrengthXp()) + " до след.)").formatted(Formatting.YELLOW), false);
                player.sendMessage(new LiteralText("Общая сила -> здоровье: "
                        + String.format(Locale.ROOT, "%.1f", GymPhysicalStats.maxHealthForStrengthLevel(strengthLevel) / 2.0D)
                        + " сердца  (ваниль: 10.0)").formatted(Formatting.YELLOW), false);
                player.sendMessage(new LiteralText("Жим Lv." + benchLevel + " -> базовый урон: "
                        + String.format(Locale.ROOT, "%.1f", GymPhysicalStats.attackDamageForBenchLevel(benchLevel))
                        + "  (ваниль: 1.0)").formatted(Formatting.RED), false);
                player.sendMessage(new LiteralText("Присед Lv." + squatLevel + " -> скорость бега: "
                        + GymPhysicalStats.runSpeedPercent(squatLevel) + "% от ванильной").formatted(Formatting.GREEN), false);
                player.sendMessage(new LiteralText("Становая Lv." + deadliftLevel + " -> высота прыжка: ~"
                        + GymPhysicalStats.jumpHeightPercent(deadliftLevel) + "% от ванильной").formatted(Formatting.AQUA), false);
                player.sendMessage(new LiteralText(StrengthDecay.statusText(data)).formatted(Formatting.DARK_GRAY), false);
                player.sendMessage(new LiteralText("Biceps: Lv." + bicepsLevel + "  (" + data.hardgym$getBicepsXp() + " XP)").formatted(Formatting.AQUA), false);
                player.sendMessage(new LiteralText("Chest: Lv." + chestLevel + "  (" + data.hardgym$getChestXp() + " XP)").formatted(Formatting.GREEN), false);
                player.sendMessage(new LiteralText("Triceps: Lv." + tricepsLevel + "  (" + data.hardgym$getTricepsXp() + " XP)").formatted(Formatting.BLUE), false);
                player.sendMessage(new LiteralText("Legs: Lv." + legsLevel + "  (" + data.hardgym$getLegsXp() + " XP)").formatted(Formatting.DARK_GREEN), false);
                player.sendMessage(new LiteralText("Stamina: " + data.hardgym$getStamina() + "%   Fatigue: " + data.hardgym$getFatigue()
                        + "%   Ночей без сна подряд: " + data.hardgym$getMissedSleepNights())
                        .formatted(Formatting.WHITE), false);
                player.sendMessage(new LiteralText("Bench PR: " + (data.hardgym$getBenchPressPr() > 0 ? data.hardgym$getBenchPressPr() + " кг" : "---")
                        + "   |   Squat PR: " + (data.hardgym$getSquatPr() > 0 ? data.hardgym$getSquatPr() + " кг" : "---")
                        + "   |   Deadlift PR: " + (data.hardgym$getDeadliftPr() > 0 ? data.hardgym$getDeadliftPr() + " кг" : "---"))
                        .formatted(Formatting.LIGHT_PURPLE), false);
                player.sendMessage(new LiteralText("Оценочный 1ПМ: жим "
                        + StrengthRepTable.roundedKg(data.hardgym$getBenchEstimated1Rm()) + " кг   |   присед "
                        + StrengthRepTable.roundedKg(data.hardgym$getSquatEstimated1Rm()) + " кг   |   становая "
                        + StrengthRepTable.roundedKg(data.hardgym$getDeadliftEstimated1Rm()) + " кг")
                        .formatted(Formatting.AQUA), false);
                player.sendMessage(new LiteralText("Bench reps: " + data.hardgym$getBenchPressReps() + "   Squat reps: " + data.hardgym$getSquatReps()
                        + "   Deadlift reps: " + data.hardgym$getDeadliftReps()
                        + "   Lifetime reps: " + data.hardgym$getTotalReps()).formatted(Formatting.GRAY), false);
                return 1;
            }));

            dispatcher.register(literal("gymkit").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                player.giveItemStack(new ItemStack(DUMBBELL_5KG));
                player.giveItemStack(new ItemStack(DUMBBELL_10KG));
                player.giveItemStack(new ItemStack(DUMBBELL_20KG));
                player.giveItemStack(new ItemStack(BENCH_PRESS_STATION));
                player.giveItemStack(new ItemStack(SQUAT_RACK));
                player.giveItemStack(new ItemStack(DEADLIFT_BAR));
                player.sendMessage(new LiteralText("HardGym v0.9.2 kit: гантели + жим + присед + становая.")
                        .formatted(Formatting.GREEN), false);
                return 1;
            }));
        });
    }

    private void registerInputNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(BENCH_INPUT_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean held = buf.readBoolean();
            server.execute(() -> BenchPressManager.setPressing(player, held));
        });
        ServerPlayNetworking.registerGlobalReceiver(SQUAT_INPUT_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean held = buf.readBoolean();
            server.execute(() -> SquatManager.setPressing(player, held));
        });
        ServerPlayNetworking.registerGlobalReceiver(DEADLIFT_INPUT_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean held = buf.readBoolean();
            server.execute(() -> DeadliftManager.setPressing(player, held));
        });
    }

    private void registerServerTick() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BenchPressManager.tick(server);
            SquatManager.tick(server);
            DeadliftManager.tick(server);

            // Sleep / sleep-deprivation system.
            // A completed night of sleep restores FAT and resets the consecutive missed-night streak.
            // If a full Minecraft night passes while the player is awake, FAT rises:
            // 1st missed night +30, 2nd +45, 3rd +60, 4th +75,
            // 5th and every later consecutive missed night +90.
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                GymPlayerData data = (GymPlayerData) player;
                long worldTime = player.getServerWorld().getTimeOfDay();
                long currentDay = worldTime / 24000L;

                // Migration / first observation: begin tracking from the current day without
                // retroactively punishing old worlds or time spent offline before v0.9.7.
                if (data.hardgym$getLastSleepCheckDay() < 0L) {
                    data.hardgym$setLastSleepCheckDay(currentDay);
                }

                if (player.isSleeping()) {
                    if (!sleepStartTime.containsKey(uuid)) {
                        sleepStartTime.put(uuid, worldTime);
                    }
                    sleepTicks.put(uuid, Math.min(400, sleepTicks.getOrDefault(uuid, 0) + 1));
                } else {
                    Integer slept = sleepTicks.remove(uuid);
                    Long startedAt = sleepStartTime.remove(uuid);

                    if (slept != null && startedAt != null) {
                        long wakeTime = player.getServerWorld().getTimeOfDay();
                        long startDay = startedAt / 24000L;
                        long wakeDay = wakeTime / 24000L;
                        long wakeTimeOfDay = wakeTime % 24000L;

                        boolean completedNightSleep =
                                wakeDay > startDay
                                && wakeTimeOfDay < 1200L
                                && slept >= 40;

                        if (completedNightSleep) {
                            int before = data.hardgym$getFatigue();
                            data.hardgym$setFatigue(before - SLEEP_FATIGUE_RESTORE);
                            int restored = before - data.hardgym$getFatigue();

                            // A real sleep breaks the sleep-deprivation streak.
                            data.hardgym$setMissedSleepNights(0);
                            data.hardgym$setLastSleepCheckDay(wakeDay);

                            if (restored > 0) {
                                player.sendMessage(new LiteralText("Полный сон восстановил " + restored + "% FAT")
                                        .formatted(Formatting.AQUA), true);
                            }
                            player.sendMessage(new LiteralText("Серия ночей без сна сброшена.")
                                    .formatted(Formatting.GREEN), true);
                            GymHud.showRecoveryOrHide(player);
                        }
                    }

                    // Only count a missed night while the player is awake. A sleeping player
                    // can cross dawn before Minecraft clears the sleeping state; that should
                    // never be mistaken for sleep deprivation.
                    long trackedDay = data.hardgym$getLastSleepCheckDay();
                    currentDay = player.getServerWorld().getTimeOfDay() / 24000L;

                    if (currentDay == trackedDay + 1L) {
                        int streak = data.hardgym$getMissedSleepNights() + 1;
                        data.hardgym$setMissedSleepNights(streak);

                        int sleepPenalty = Math.min(90, 30 + (streak - 1) * 15);
                        int before = data.hardgym$getFatigue();
                        data.hardgym$setFatigue(before + sleepPenalty);
                        int actuallyAdded = data.hardgym$getFatigue() - before;
                        data.hardgym$setLastSleepCheckDay(currentDay);

                        if (actuallyAdded > 0) {
                            player.sendMessage(new LiteralText("Не выспался: +" + actuallyAdded
                                    + "% FAT (" + streak + "-я ночь подряд, штраф "
                                    + sleepPenalty + "%)")
                                    .formatted(Formatting.RED), true);
                        } else {
                            player.sendMessage(new LiteralText("Не выспался: FAT уже на максимуме ("
                                    + streak + "-я ночь подряд).")
                                    .formatted(Formatting.RED), true);
                        }
                        GymHud.showRecoveryOrHide(player);
                    } else if (currentDay > trackedDay + 1L) {
                        // Large jumps usually mean an old save, an offline server gap, or /time.
                        // Sync to the current day without stacking several unseen missed nights.
                        data.hardgym$setLastSleepCheckDay(currentDay);
                    }
                }
            }

            recoveryTicker++;
            if (recoveryTicker < 20) return;
            recoveryTicker = 0;
            fatigueRecoverySeconds++;
            boolean recoverFatigueNow = fatigueRecoverySeconds >= FATIGUE_PASSIVE_INTERVAL_SECONDS;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                StrengthDecay.tickOneSecond(player);
                GymAttributeManager.update(player);
                syncPhysique(player);
                if (player.isUsingItem() && player.getActiveItem().getItem() instanceof DumbbellItem) continue;
                if (BenchPressManager.isTraining(player.getUuid()) || SquatManager.isTraining(player.getUuid())
                        || DeadliftManager.isTraining(player.getUuid())) continue;

                GymPlayerData data = (GymPlayerData) player;
                int staminaBeforeRecovery = data.hardgym$getStamina();
                data.hardgym$setStamina(data.hardgym$getStamina() + 3);

                // FAT is deliberately much slower than stamina: only 1% every 10 seconds.
                if (recoverFatigueNow && !player.isSleeping()) {
                    data.hardgym$setFatigue(data.hardgym$getFatigue() - 1);
                }

                if (data.hardgym$getStamina() < 100) GymHud.showRecovery(player);
                else if (staminaBeforeRecovery < 100) GymHud.hide(player);
            }

            if (recoverFatigueNow) fatigueRecoverySeconds = 0;
        });
    }

    private static void syncPhysique(ServerPlayerEntity player) {
        GymPlayerData data = (GymPlayerData) player;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(GymStats.levelForXp(data.hardgym$getStrengthXp()));
        buf.writeInt(GymStats.levelForXp(data.hardgym$getBenchXp()));
        buf.writeInt(GymStats.levelForXp(data.hardgym$getSquatXp()));
        buf.writeInt(GymStats.levelForXp(data.hardgym$getDeadliftXp()));
        ServerPlayNetworking.send(player, PHYSIQUE_SYNC_PACKET, buf);
    }

    private void registerRespawnCopy() {
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            GymPlayerData oldData = (GymPlayerData) oldPlayer;
            GymPlayerData newData = (GymPlayerData) newPlayer;
            newData.hardgym$setStrengthXp(oldData.hardgym$getStrengthXp());
            newData.hardgym$setStrengthIdleSeconds(oldData.hardgym$getStrengthIdleSeconds());
            newData.hardgym$setBicepsXp(oldData.hardgym$getBicepsXp());
            newData.hardgym$setChestXp(oldData.hardgym$getChestXp());
            newData.hardgym$setTricepsXp(oldData.hardgym$getTricepsXp());
            newData.hardgym$setLegsXp(oldData.hardgym$getLegsXp());
            newData.hardgym$setBenchXp(oldData.hardgym$getBenchXp());
            newData.hardgym$setSquatXp(oldData.hardgym$getSquatXp());
            newData.hardgym$setDeadliftXp(oldData.hardgym$getDeadliftXp());
            newData.hardgym$setStamina(oldData.hardgym$getStamina());
            newData.hardgym$setFatigue(oldData.hardgym$getFatigue());
            newData.hardgym$setMissedSleepNights(oldData.hardgym$getMissedSleepNights());
            newData.hardgym$setLastSleepCheckDay(oldData.hardgym$getLastSleepCheckDay());
            newData.hardgym$setTotalReps(oldData.hardgym$getTotalReps());
            newData.hardgym$setBenchPressReps(oldData.hardgym$getBenchPressReps());
            newData.hardgym$setBenchPressPr(oldData.hardgym$getBenchPressPr());
            newData.hardgym$setSquatReps(oldData.hardgym$getSquatReps());
            newData.hardgym$setSquatPr(oldData.hardgym$getSquatPr());
            newData.hardgym$setDeadliftReps(oldData.hardgym$getDeadliftReps());
            newData.hardgym$setDeadliftPr(oldData.hardgym$getDeadliftPr());
            newData.hardgym$setBenchEstimated1Rm(oldData.hardgym$getBenchEstimated1Rm());
            newData.hardgym$setSquatEstimated1Rm(oldData.hardgym$getSquatEstimated1Rm());
            newData.hardgym$setDeadliftEstimated1Rm(oldData.hardgym$getDeadliftEstimated1Rm());
            BenchPressManager.clear(oldPlayer.getUuid());
            SquatManager.clear(oldPlayer.getUuid());
            DeadliftManager.clear(oldPlayer.getUuid());
        });
    }
}
