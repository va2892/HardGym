package com.hardgym.training;

import com.hardgym.HardGymMod;
import com.hardgym.block.BenchPressBlock;
import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
import com.hardgym.player.GymStats;
import com.hardgym.player.GymPhysicalStats;
import com.hardgym.player.StrengthDecay;
import com.hardgym.player.StrengthRepTable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BenchPressManager {
    private static final Map<UUID, BenchSession> SESSIONS = new HashMap<>();
    private static final int[] TRAINING_WEIGHTS = new int[]{40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 220};

    private BenchPressManager() {
    }

    public static boolean isTraining(UUID uuid) {
        return SESSIONS.containsKey(uuid);
    }

    public static void setPressing(ServerPlayerEntity player, boolean pressing) {
        BenchSession session = SESSIONS.get(player.getUuid());
        if (session != null && !session.pinned) {
            session.pressing = pressing;
        }
    }

    public static void clear(UUID uuid) {
        BenchSession session = SESSIONS.remove(uuid);
        if (session != null) {
            setBenchAnimation(session.world, session.pos, 0);
            setBenchActive(session.world, session.pos, false);
        }
    }

    public static void toggle(ServerPlayerEntity player, BlockPos pos, int weightKg) {
        UUID uuid = player.getUuid();
        BenchSession current = SESSIONS.get(uuid);

        if (current != null) {
            stop(player, current, "Подход закончен: " + current.reps + " повт. × " + current.weightKg + " кг");
            return;
        }

        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int required = requiredStrength(weightKg);

        if (strengthLevel < required) {
            player.sendMessage(new LiteralText("Слишком тяжело для жима. Нужен Strength " + required + ".")
                    .formatted(Formatting.RED), true);
            return;
        }

        double oneRepMax = effectiveOneRepMax(data, strengthLevel, weightKg);
        int targetReps = StrengthRepTable.targetRepsWithFatigue(weightKg, oneRepMax, data.hardgym$getFatigue());
        int staminaCost = StrengthRepTable.staminaCostForTarget(targetReps);
        if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
            player.sendMessage(new LiteralText("Ты слишком устал для подхода. Отдохни.")
                    .formatted(Formatting.RED), true);
            return;
        }

        int ticksPerRep = ticksPerRep(player, weightKg);
        BenchSession session = new BenchSession(player, player.getServerWorld(), pos, weightKg, ticksPerRep, targetReps);
        SESSIONS.put(uuid, session);
        setBenchAnimation(session.world, pos, 0);
        setBenchActive(session.world, pos, true);
        lockPlayerToBench(player, session);
        playUnrackSound(session);

    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, BenchSession>> iterator = SESSIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BenchSession> entry = iterator.next();
            BenchSession session = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null || !player.isAlive()) {
                setBenchAnimation(session.world, session.pos, 0);
                setBenchActive(session.world, session.pos, false);
                if (player != null) {
                    GymHud.showRecoveryOrHide(player);
                }
                iterator.remove();
                continue;
            }

            if (player.isSneaking()) {
                if (session.pinned) {
                    playDroppedBarSound(session);
                } else {
                    playRackSound(session);
                }
                releasePlayer(player, session);
                setBenchAnimation(session.world, session.pos, 0);
                setBenchActive(session.world, session.pos, false);
                player.sendMessage(new LiteralText("Подход закончен: " + session.reps + " повт. × " + session.weightKg + " кг")
                        .formatted(Formatting.GRAY), true);
                GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            if (player.getServerWorld() != session.world
                    || session.world.getBlockState(session.pos).getBlock() != HardGymMod.BENCH_PRESS_STATION) {
                releasePlayer(player, session);
                player.sendMessage(new LiteralText("Подход остановлен: скамья недоступна.")
                        .formatted(Formatting.GRAY), true);
                setBenchAnimation(session.world, session.pos, 0);
                setBenchActive(session.world, session.pos, false);
                GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            lockPlayerToBench(player, session);
            GymPlayerData data = (GymPlayerData) player;

            if (!session.pinned) {
                int holdProgress = 100 - (session.ticksRemaining * 100 / Math.max(1, session.ticksPerRep));
                int holdAnim = animationForProgress(holdProgress);

                // Dynamic reps are now paid for at the end of each rep according to %1RM.
                // Static holding still drains stamina by bar position, but only while RMB is released.
                if (!session.pressing) {
                    session.holdDrainTicker++;
                    if (session.holdDrainTicker >= 20) {
                        session.holdDrainTicker = 0;
                        data.hardgym$setStamina(data.hardgym$getStamina()
                                - holdingStaminaDrain(session.weightKg, holdAnim));
                        player.getHungerManager().addExhaustion(holdingHungerExhaustion(session.weightKg, holdAnim));
                    }
                } else {
                    session.holdDrainTicker = 0;
                }

                if (data.hardgym$getStamina() <= 0) {
                    enterPinned(player, session);
                }
            }

            if (session.pinned) {
                setBenchAnimation(session.world, session.pos, 2);
                session.lastAnim = 2;
                session.damageTicker++;

                if (session.damageTicker >= 20) {
                    session.damageTicker = 0;
                    player.damage(DamageSource.ANVIL, crushDamage(session.weightKg));
                    // Being trapped under the bar is extremely taxing: FAT rises quickly
                    // for every full second the player stays pinned.
                    data.hardgym$setFatigue(data.hardgym$getFatigue()
                            + pinnedFatiguePerSecond(session.weightKg));
                }

                session.hudTicker++;
                if (session.hudTicker >= 4) {
                    session.hudTicker = 0;
                    GymHud.showBenchPinned(player, session.weightKg, session.reps,
                            data.hardgym$getFatigue(), Math.round(player.getHealth()));
                }
                continue;
            }

            int progress = 100 - (session.ticksRemaining * 100 / Math.max(1, session.ticksPerRep));

            if (session.pressing) {
                session.ticksRemaining--;
                progress = 100 - (session.ticksRemaining * 100 / Math.max(1, session.ticksPerRep));

                int anim = animationForProgress(progress);
                if (anim != session.lastAnim) {
                    playMovementSound(session, session.lastAnim, anim, progress);
                    setBenchAnimation(session.world, session.pos, anim);
                    session.lastAnim = anim;
                }

                if (session.ticksRemaining <= 0) {
                    if (!performRep(player, session)) {
                        enterPinned(player, session);
                        continue;
                    }

                    session.ticksPerRep = ticksPerRep(player, session.weightKg);
                    session.ticksRemaining = session.ticksPerRep;
                    setBenchAnimation(session.world, session.pos, 0);
                    session.lastAnim = 0;
                    progress = 0;
                }
            }

            session.hudTicker++;
            if (session.hudTicker >= 4) {
                session.hudTicker = 0;
                if (session.pressing) {
                    GymHud.showBench(player, session.weightKg, session.reps,
                            data.hardgym$getStamina(), data.hardgym$getFatigue(), progress);
                } else {
                    GymHud.showBenchWaiting(player, session.weightKg, session.reps,
                            data.hardgym$getStamina(), data.hardgym$getFatigue(), progress);
                }
            }
        }
    }

    public static void stopForPlayer(ServerPlayerEntity player) {
        BenchSession current = SESSIONS.remove(player.getUuid());
        if (current != null) {
            if (current.pinned) {
                playDroppedBarSound(current);
            } else {
                playRackSound(current);
            }
            releasePlayer(player, current);
            setBenchAnimation(current.world, current.pos, 0);
            setBenchActive(current.world, current.pos, false);
            player.sendMessage(new LiteralText("Подход закончен: " + current.reps + " повт. × " + current.weightKg + " кг")
                    .formatted(Formatting.GRAY), true);
            GymHud.showRecoveryOrHide(player);
        }
    }

    private static void stop(ServerPlayerEntity player, BenchSession session, String message) {
        SESSIONS.remove(player.getUuid());
        if (session.pinned) {
            playDroppedBarSound(session);
        } else {
            playRackSound(session);
        }
        releasePlayer(player, session);
        setBenchAnimation(session.world, session.pos, 0);
        setBenchActive(session.world, session.pos, false);
        player.sendMessage(new LiteralText(message).formatted(Formatting.GRAY), true);
        GymHud.showRecoveryOrHide(player);
    }

    private static void enterPinned(ServerPlayerEntity player, BenchSession session) {
        if (session.pinned) {
            return;
        }

        session.pinned = true;
        session.pressing = false;
        session.damageTicker = 0;

        GymPlayerData data = (GymPlayerData) player;
        int instantFatigue = pinnedFatigueInstant(session.weightKg);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + instantFatigue);

        setBenchAnimation(session.world, session.pos, 2);
        session.lastAnim = 2;
        playDroppedBarSound(session);

        player.sendMessage(new LiteralText("ТЕБЯ ПРИДАВИЛО " + session.weightKg + " КГ! +"
                + instantFatigue + "% FAT. Shift — сбросить штангу.")
                .formatted(Formatting.DARK_RED), false);
    }

    private static void lockPlayerToBench(ServerPlayerEntity player, BenchSession session) {
        BlockState state = session.world.getBlockState(session.pos);
        if (state.getBlock() != HardGymMod.BENCH_PRESS_STATION) {
            return;
        }

        Direction headDirection = BenchPressBlock.getFacing(state);
        float yaw = headDirection.asRotation();

        double x = session.pos.getX() + 0.5D;
        double y = session.pos.getY() + 0.34D;
        double z = session.pos.getZ() + 0.5D;

        x += headDirection.getOffsetX() * 0.80D;
        z += headDirection.getOffsetZ() * 0.80D;

        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
        player.networkHandler.requestTeleport(x, y, z, yaw, 0.0F);
        player.setBodyYaw(yaw);
        player.setHeadYaw(yaw);
    }

    private static void releasePlayer(ServerPlayerEntity player, BenchSession session) {
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.refreshPositionAndAngles(
                session.startX,
                session.startY,
                session.startZ,
                session.startYaw,
                session.startPitch
        );
        player.networkHandler.requestTeleport(
                session.startX,
                session.startY,
                session.startZ,
                session.startYaw,
                session.startPitch
        );
        player.setBodyYaw(session.startYaw);
        player.setHeadYaw(session.startYaw);
    }

    private static boolean performRep(ServerPlayerEntity player, BenchSession session) {
        GymPlayerData data = (GymPlayerData) player;
        int required = requiredStrength(session.weightKg);
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int staminaCost = StrengthRepTable.staminaCostForTarget(session.targetReps);
        int fatigueGain = StrengthRepTable.fatigueGainForRep(session.targetReps, session.reps);

        if (strengthLevel < required || session.reps >= session.targetReps
                || data.hardgym$getStamina() < staminaCost) {
            return false;
        }

        int oldStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int oldChestLevel = GymStats.levelForXp(data.hardgym$getChestXp());
        int oldTricepsLevel = GymStats.levelForXp(data.hardgym$getTricepsXp());
        int oldBenchLevel = GymStats.levelForXp(data.hardgym$getBenchXp());

        data.hardgym$setStamina(data.hardgym$getStamina() - staminaCost);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + fatigueGain);
        data.hardgym$setStrengthXp(data.hardgym$getStrengthXp() + strengthXp(session.weightKg));
        StrengthDecay.markTraining(player);
        data.hardgym$setChestXp(data.hardgym$getChestXp() + chestXp(session.weightKg));
        data.hardgym$setBenchXp(data.hardgym$getBenchXp() + chestXp(session.weightKg));
        data.hardgym$setTricepsXp(data.hardgym$getTricepsXp() + tricepsXp(session.weightKg));
        data.hardgym$setTotalReps(data.hardgym$getTotalReps() + 1);
        data.hardgym$setBenchPressReps(data.hardgym$getBenchPressReps() + 1);
        session.reps++;

        double setEstimate = StrengthRepTable.estimateOneRepMax(session.weightKg, session.reps);
        if (setEstimate > data.hardgym$getBenchEstimated1Rm()) {
            data.hardgym$setBenchEstimated1Rm(setEstimate);
        }

        // Полный повтор дополнительно расходует энергию/голод.
        player.getHungerManager().addExhaustion(benchRepHungerExhaustion(session.weightKg));

        if (session.weightKg > data.hardgym$getBenchPressPr()) {
            data.hardgym$setBenchPressPr(session.weightKg);
            player.sendMessage(new LiteralText("НОВЫЙ PR В ЖИМЕ: " + session.weightKg + " кг!")
                    .formatted(Formatting.LIGHT_PURPLE), false);
        }

        int newStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int newChestLevel = GymStats.levelForXp(data.hardgym$getChestXp());
        int newTricepsLevel = GymStats.levelForXp(data.hardgym$getTricepsXp());
        int newBenchLevel = GymStats.levelForXp(data.hardgym$getBenchXp());

        if (newStrengthLevel > oldStrengthLevel) {
            player.sendMessage(new LiteralText("HardGym: Strength теперь " + newStrengthLevel + "!")
                    .formatted(Formatting.GOLD), false);
        }
        if (newChestLevel > oldChestLevel) {
            player.sendMessage(new LiteralText("HardGym: Chest теперь " + newChestLevel + "!")
                    .formatted(Formatting.AQUA), false);
        }
        if (newTricepsLevel > oldTricepsLevel) {
            player.sendMessage(new LiteralText("HardGym: Triceps теперь " + newTricepsLevel + "!")
                    .formatted(Formatting.BLUE), false);
        }
        if (newBenchLevel > oldBenchLevel) {
            player.sendMessage(new LiteralText("HardGym: Жим Lv." + newBenchLevel + " — базовый урон "
                    + String.format(java.util.Locale.ROOT, "%.1f", GymPhysicalStats.attackDamageForBenchLevel(newBenchLevel)))
                    .formatted(Formatting.RED), false);
        }

        return true;
    }

    private static int requiredStrength(int weightKg) {
        if (weightKg <= 40) return 2;
        if (weightKg <= 50) return 3;
        if (weightKg <= 60) return 3;
        if (weightKg <= 70) return 4;
        if (weightKg <= 80) return 5;
        if (weightKg <= 90) return 6;
        if (weightKg <= 100) return 7;
        if (weightKg <= 120) return 10;
        if (weightKg <= 140) return 12;
        if (weightKg <= 160) return 14;
        if (weightKg <= 180) return 16;
        if (weightKg <= 200) return 18;
        return 20;
    }

    private static double effectiveOneRepMax(GymPlayerData data, int strengthLevel, int selectedWeightKg) {
        int maxUnlocked = 0;
        for (int weight : TRAINING_WEIGHTS) {
            if (strengthLevel >= requiredStrength(weight)) maxUnlocked = weight;
        }
        double progressionFloor = maxUnlocked > 0 ? maxUnlocked / 0.85D : selectedWeightKg / 0.85D;
        double stored = data.hardgym$getBenchEstimated1Rm();
        if (stored <= 0.0D && data.hardgym$getBenchPressPr() > 0) {
            stored = StrengthRepTable.migrateOldPr(data.hardgym$getBenchPressPr());
        }
        double effective = Math.max(progressionFloor, Math.max(stored, selectedWeightKg));
        if (effective > data.hardgym$getBenchEstimated1Rm()) data.hardgym$setBenchEstimated1Rm(effective);
        return effective;
    }

    private static int holdingStaminaDrain(int weightKg, int anim) {
        int base;
        if (weightKg <= 80) {
            base = 1;
        } else if (weightKg <= 140) {
            base = 2;
        } else {
            base = 3;
        }

        // anim 0 = верхняя точка, anim 1 = середина, anim 2 = у груди.
        // Чем ниже штанга, тем больше статическая нагрузка на игрока.
        if (anim == 2) {
            return base + 4;
        }
        if (anim == 1) {
            return base + 2;
        }
        return base;
    }

    private static float holdingHungerExhaustion(int weightKg, int anim) {
        // Расход за каждую секунду удержания штанги.
        float base = 0.08F + weightKg / 1800.0F;
        if (anim == 2) {
            return base * 1.50F; // у груди тяжелее всего
        }
        if (anim == 1) {
            return base * 1.25F;
        }
        return base; // верхняя точка
    }

    private static float benchRepHungerExhaustion(int weightKg) {
        // Усиленный расход: 40 кг ~1.5, 100 кг ~2.1, 200 кг ~3.1 exhaustion за полный повтор.
        return 1.10F + weightKg / 100.0F;
    }

    private static float crushDamage(int weightKg) {
        if (weightKg <= 80) return 1.0F;
        if (weightKg <= 120) return 2.0F;
        if (weightKg <= 160) return 3.0F;
        return 4.0F;
    }

    private static int pinnedFatigueInstant(int weightKg) {
        // Immediate shock/stress from failing the lift: about 9–13 FAT on bench weights.
        return Math.min(18, 8 + weightKg / 40);
    }

    private static int pinnedFatiguePerSecond(int weightKg) {
        // Continuous strain while trapped: roughly +2 to +4 FAT every second on bench.
        return Math.min(6, 2 + weightKg / 100);
    }


    private static int strengthXp(int weightKg) {
        return Math.max(6, weightKg / 4);
    }

    private static int chestXp(int weightKg) {
        return Math.max(10, weightKg / 2);
    }

    private static int tricepsXp(int weightKg) {
        return Math.max(8, weightKg / 3);
    }

    private static int ticksPerRep(ServerPlayerEntity player, int weightKg) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int required = requiredStrength(weightKg);
        int extraStrength = Math.max(0, strengthLevel - required);
        int base = 38 + weightKg / 4;
        return Math.max(22, base - extraStrength * 3);
    }

    private static void playUnrackSound(BenchSession session) {
        float pitch = weightPitch(session.weightKg) + 0.08F;
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_PLACE,
                SoundCategory.BLOCKS, 0.75F, pitch);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,
                SoundCategory.BLOCKS, 0.45F, pitch + 0.05F);
    }

    private static void playRackSound(BenchSession session) {
        float pitch = weightPitch(session.weightKg);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_PLACE,
                SoundCategory.BLOCKS, 0.85F, pitch);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
                SoundCategory.BLOCKS, 0.60F, pitch - 0.05F);
    }

    private static void playDroppedBarSound(BenchSession session) {
        float pitch = Math.max(0.55F, weightPitch(session.weightKg) - 0.12F);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_FALL,
                SoundCategory.BLOCKS, 1.0F, pitch);
    }

    private static void playMovementSound(BenchSession session, int oldAnim, int newAnim, int progress) {
        float pitch = weightPitch(session.weightKg);

        // 0 -> 1 on the first half of the rep = lowering the bar.
        if (oldAnim == 0 && newAnim == 1 && progress < 50) {
            session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_STEP,
                    SoundCategory.BLOCKS, 0.32F, pitch - 0.08F);
            return;
        }

        // 2 -> 1 on the second half = pressing the bar away from the chest.
        if (oldAnim == 2 && newAnim == 1 && progress >= 50) {
            session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_HIT,
                    SoundCategory.BLOCKS, 0.42F, pitch + 0.06F);
        }
    }

    private static float weightPitch(int weightKg) {
        // Heavier bars sound a little deeper. 40 kg ~= 1.0, 200 kg ~= 0.68.
        return Math.max(0.68F, 1.08F - (weightKg - 40) * 0.0025F);
    }

    private static int animationForProgress(int progressPercent) {
        if (progressPercent < 20) {
            return 0;
        }
        if (progressPercent < 45) {
            return 1;
        }
        if (progressPercent < 70) {
            return 2;
        }
        if (progressPercent < 90) {
            return 1;
        }
        return 0;
    }

    private static void setBenchAnimation(ServerWorld world, BlockPos pos, int anim) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != HardGymMod.BENCH_PRESS_STATION) {
            return;
        }
        if (state.get(BenchPressBlock.ANIM) != anim) {
            world.setBlockState(pos, state.with(BenchPressBlock.ANIM, anim), 3);
        }
    }

    private static void setBenchActive(ServerWorld world, BlockPos pos, boolean active) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != HardGymMod.BENCH_PRESS_STATION) {
            return;
        }
        if (state.get(BenchPressBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(BenchPressBlock.ACTIVE, active), 3);
        }
    }

    private static final class BenchSession {
        private final UUID uuid;
        private final ServerWorld world;
        private final BlockPos pos;
        private final int weightKg;
        private final double startX;
        private final double startY;
        private final double startZ;
        private final float startYaw;
        private final float startPitch;
        private int ticksPerRep;
        private int ticksRemaining;
        private int reps;
        private final int targetReps;
        private int lastAnim;
        private boolean pressing;
        private boolean pinned;
        private int holdDrainTicker;
        private int damageTicker;
        private int hudTicker;

        private BenchSession(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int weightKg, int ticksPerRep, int targetReps) {
            this.uuid = player.getUuid();
            this.world = world;
            this.pos = pos.toImmutable();
            this.weightKg = weightKg;
            this.startX = player.getX();
            this.startY = player.getY();
            this.startZ = player.getZ();
            this.startYaw = player.yaw;
            this.startPitch = player.pitch;
            this.ticksPerRep = ticksPerRep;
            this.ticksRemaining = ticksPerRep;
            this.reps = 0;
            this.targetReps = Math.max(1, targetReps);
            this.lastAnim = 0;
            this.pressing = false;
            this.pinned = false;
            this.holdDrainTicker = 0;
            this.damageTicker = 0;
            this.hudTicker = 0;
        }
    }
}
