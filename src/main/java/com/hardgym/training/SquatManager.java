package com.hardgym.training;

import com.hardgym.HardGymMod;
import com.hardgym.block.SquatRackBlock;
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

public final class SquatManager {
    private static final Map<UUID, SquatSession> SESSIONS = new HashMap<>();
    private static final int[] TRAINING_WEIGHTS = new int[]{40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 220, 240, 260, 280, 300};

    private SquatManager() {
    }

    public static boolean isTraining(UUID uuid) {
        return SESSIONS.containsKey(uuid);
    }

    public static void setPressing(ServerPlayerEntity player, boolean pressing) {
        SquatSession session = SESSIONS.get(player.getUuid());
        if (session != null && !session.pinned) {
            session.pressing = pressing;
        }
    }

    public static void clear(UUID uuid) {
        SquatSession session = SESSIONS.remove(uuid);
        if (session != null) {
            setAnimation(session.world, session.pos, 0);
            setActive(session.world, session.pos, false);
        }
    }

    public static void toggle(ServerPlayerEntity player, BlockPos pos, int weightKg) {
        UUID uuid = player.getUuid();
        SquatSession current = SESSIONS.get(uuid);

        if (current != null) {
            stop(player, current, "Подход приседа закончен: " + current.reps + " повт. × " + current.weightKg + " кг");
            return;
        }

        if (BenchPressManager.isTraining(uuid) || DeadliftManager.isTraining(uuid)) {
            player.sendMessage(new LiteralText("Сначала закончи жим лёжа.").formatted(Formatting.RED), true);
            return;
        }

        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int required = requiredStrength(weightKg);

        if (strengthLevel < required) {
            player.sendMessage(new LiteralText("Слишком тяжело для приседа. Нужен Strength " + required + ".")
                    .formatted(Formatting.RED), true);
            return;
        }

        double oneRepMax = effectiveOneRepMax(data, strengthLevel, weightKg);
        int targetReps = StrengthRepTable.targetRepsWithFatigue(weightKg, oneRepMax, data.hardgym$getFatigue());
        int staminaCost = StrengthRepTable.staminaCostForTarget(targetReps);
        if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
            player.sendMessage(new LiteralText("Ты слишком устал для приседа. Отдохни.")
                    .formatted(Formatting.RED), true);
            return;
        }

        int ticksPerRep = ticksPerRep(player, weightKg);
        SquatSession session = new SquatSession(player, player.getServerWorld(), pos, weightKg, ticksPerRep, targetReps);
        SESSIONS.put(uuid, session);
        setAnimation(session.world, pos, 0);
        setActive(session.world, pos, true);
        lockPlayer(player, session);
        playUnrackSound(session);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SquatSession>> iterator = SESSIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SquatSession> entry = iterator.next();
            SquatSession session = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null || !player.isAlive()) {
                setAnimation(session.world, session.pos, 0);
                setActive(session.world, session.pos, false);
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
                setAnimation(session.world, session.pos, 0);
                setActive(session.world, session.pos, false);
                player.sendMessage(new LiteralText("Подход приседа закончен: " + session.reps + " повт. × " + session.weightKg + " кг")
                        .formatted(Formatting.GRAY), true);
                GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            if (player.getServerWorld() != session.world
                    || session.world.getBlockState(session.pos).getBlock() != HardGymMod.SQUAT_RACK) {
                releasePlayer(player, session);
                player.sendMessage(new LiteralText("Подход остановлен: стойка недоступна.")
                        .formatted(Formatting.GRAY), true);
                setAnimation(session.world, session.pos, 0);
                setActive(session.world, session.pos, false);
                GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            lockPlayer(player, session);
            GymPlayerData data = (GymPlayerData) player;

            if (!session.pinned) {
                int holdProgress = 100 - (session.ticksRemaining * 100 / Math.max(1, session.ticksPerRep));
                int holdAnim = animationForProgress(holdProgress);

                // Rep stamina is based on relative %1RM. Static holding remains position-dependent.
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
                setAnimation(session.world, session.pos, 2);
                session.lastAnim = 2;
                session.damageTicker++;

                if (session.damageTicker >= 20) {
                    session.damageTicker = 0;
                    player.damage(DamageSource.ANVIL, crushDamage(session.weightKg));
                    // Staying crushed under a failed squat rapidly builds persistent fatigue.
                    data.hardgym$setFatigue(data.hardgym$getFatigue()
                            + pinnedFatiguePerSecond(session.weightKg));
                }

                session.hudTicker++;
                if (session.hudTicker >= 4) {
                    session.hudTicker = 0;
                    GymHud.showSquatPinned(player, session.weightKg, session.reps,
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
                    setAnimation(session.world, session.pos, anim);
                    session.lastAnim = anim;
                }

                if (session.ticksRemaining <= 0) {
                    if (!performRep(player, session)) {
                        enterPinned(player, session);
                        continue;
                    }

                    session.ticksPerRep = ticksPerRep(player, session.weightKg);
                    session.ticksRemaining = session.ticksPerRep;
                    setAnimation(session.world, session.pos, 0);
                    session.lastAnim = 0;
                    progress = 0;
                }
            }

            session.hudTicker++;
            if (session.hudTicker >= 4) {
                session.hudTicker = 0;
                if (session.pressing) {
                    GymHud.showSquat(player, session.weightKg, session.reps,
                            data.hardgym$getStamina(), data.hardgym$getFatigue(), progress);
                } else {
                    GymHud.showSquatWaiting(player, session.weightKg, session.reps,
                            data.hardgym$getStamina(), data.hardgym$getFatigue(), progress);
                }
            }
        }
    }

    public static void stopForPlayer(ServerPlayerEntity player) {
        SquatSession current = SESSIONS.remove(player.getUuid());
        if (current != null) {
            if (current.pinned) playDroppedBarSound(current); else playRackSound(current);
            releasePlayer(player, current);
            setAnimation(current.world, current.pos, 0);
            setActive(current.world, current.pos, false);
            player.sendMessage(new LiteralText("Подход приседа закончен: " + current.reps + " повт. × " + current.weightKg + " кг")
                    .formatted(Formatting.GRAY), true);
            GymHud.showRecoveryOrHide(player);
        }
    }

    private static void stop(ServerPlayerEntity player, SquatSession session, String message) {
        SESSIONS.remove(player.getUuid());
        if (session.pinned) playDroppedBarSound(session); else playRackSound(session);
        releasePlayer(player, session);
        setAnimation(session.world, session.pos, 0);
        setActive(session.world, session.pos, false);
        player.sendMessage(new LiteralText(message).formatted(Formatting.GRAY), true);
        GymHud.showRecoveryOrHide(player);
    }

    private static void enterPinned(ServerPlayerEntity player, SquatSession session) {
        if (session.pinned) return;
        session.pinned = true;
        session.pressing = false;
        session.damageTicker = 0;

        GymPlayerData data = (GymPlayerData) player;
        int instantFatigue = pinnedFatigueInstant(session.weightKg);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + instantFatigue);

        setAnimation(session.world, session.pos, 2);
        session.lastAnim = 2;
        playDroppedBarSound(session);
        player.sendMessage(new LiteralText("НЕ МОЖЕШЬ ВСТАТЬ С " + session.weightKg + " КГ! +"
                + instantFatigue + "% FAT. Shift — сбросить штангу.")
                .formatted(Formatting.DARK_RED), false);
    }

    private static void lockPlayer(ServerPlayerEntity player, SquatSession session) {
        BlockState state = session.world.getBlockState(session.pos);
        if (state.getBlock() != HardGymMod.SQUAT_RACK) return;

        Direction facing = SquatRackBlock.getFacing(state);
        float yaw = facing.asRotation() + 180.0F;
        double standOffset = 0.34D;
        double x = session.pos.getX() + 0.5D + facing.getOffsetX() * standOffset;
        double y = session.pos.getY();
        double z = session.pos.getZ() + 0.5D + facing.getOffsetZ() * standOffset;

        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
        player.networkHandler.requestTeleport(x, y, z, yaw, 0.0F);
        player.setBodyYaw(yaw);
        player.setHeadYaw(yaw);
    }

    private static void releasePlayer(ServerPlayerEntity player, SquatSession session) {
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.refreshPositionAndAngles(session.startX, session.startY, session.startZ, session.startYaw, session.startPitch);
        player.networkHandler.requestTeleport(session.startX, session.startY, session.startZ, session.startYaw, session.startPitch);
        player.setBodyYaw(session.startYaw);
        player.setHeadYaw(session.startYaw);
    }

    private static boolean performRep(ServerPlayerEntity player, SquatSession session) {
        GymPlayerData data = (GymPlayerData) player;
        int required = requiredStrength(session.weightKg);
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int staminaCost = StrengthRepTable.staminaCostForTarget(session.targetReps);
        int fatigueGain = StrengthRepTable.fatigueGainForRep(session.targetReps, session.reps);

        if (strengthLevel < required || session.reps >= session.targetReps
                || data.hardgym$getStamina() < staminaCost) return false;

        int oldStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int oldLegsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
        int oldSquatLevel = GymStats.levelForXp(data.hardgym$getSquatXp());

        data.hardgym$setStamina(data.hardgym$getStamina() - staminaCost);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + fatigueGain);
        data.hardgym$setStrengthXp(data.hardgym$getStrengthXp() + strengthXp(session.weightKg));
        data.hardgym$setLegsXp(data.hardgym$getLegsXp() + legsXp(session.weightKg));
        data.hardgym$setSquatXp(data.hardgym$getSquatXp() + legsXp(session.weightKg));
        StrengthDecay.markTraining(player);
        data.hardgym$setTotalReps(data.hardgym$getTotalReps() + 1);
        data.hardgym$setSquatReps(data.hardgym$getSquatReps() + 1);
        session.reps++;

        double setEstimate = StrengthRepTable.estimateOneRepMax(session.weightKg, session.reps);
        if (setEstimate > data.hardgym$getSquatEstimated1Rm()) {
            data.hardgym$setSquatEstimated1Rm(setEstimate);
        }

        player.getHungerManager().addExhaustion(squatRepHungerExhaustion(session.weightKg));

        if (session.weightKg > data.hardgym$getSquatPr()) {
            data.hardgym$setSquatPr(session.weightKg);
            player.sendMessage(new LiteralText("НОВЫЙ PR В ПРИСЕДЕ: " + session.weightKg + " кг!")
                    .formatted(Formatting.LIGHT_PURPLE), false);
        }

        int newStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int newLegsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
        int newSquatLevel = GymStats.levelForXp(data.hardgym$getSquatXp());
        if (newStrengthLevel > oldStrengthLevel) {
            player.sendMessage(new LiteralText("HardGym: Strength теперь " + newStrengthLevel + "!")
                    .formatted(Formatting.GOLD), false);
        }
        if (newLegsLevel > oldLegsLevel) {
            player.sendMessage(new LiteralText("HardGym: Legs теперь " + newLegsLevel + "!")
                    .formatted(Formatting.GREEN), false);
        }
        if (newSquatLevel > oldSquatLevel) {
            player.sendMessage(new LiteralText("HardGym: Присед Lv." + newSquatLevel + " — скорость "
                    + GymPhysicalStats.runSpeedPercent(newSquatLevel) + "% от ванильной")
                    .formatted(Formatting.GREEN), false);
        }
        return true;
    }

    public static int requiredStrength(int weightKg) {
        if (weightKg <= 50) return 1;
        if (weightKg <= 70) return 2;
        if (weightKg <= 90) return 3;
        if (weightKg <= 100) return 4;
        if (weightKg <= 120) return 6;
        if (weightKg <= 140) return 8;
        if (weightKg <= 160) return 10;
        if (weightKg <= 180) return 13;
        if (weightKg <= 200) return 16;
        if (weightKg <= 220) return 18;
        if (weightKg <= 240) return 20;
        if (weightKg <= 260) return 22;
        if (weightKg <= 280) return 24;
        return 26;
    }

    private static double effectiveOneRepMax(GymPlayerData data, int strengthLevel, int selectedWeightKg) {
        int maxUnlocked = 0;
        for (int weight : TRAINING_WEIGHTS) {
            if (strengthLevel >= requiredStrength(weight)) maxUnlocked = weight;
        }
        double progressionFloor = maxUnlocked > 0 ? maxUnlocked / 0.85D : selectedWeightKg / 0.85D;
        double stored = data.hardgym$getSquatEstimated1Rm();
        if (stored <= 0.0D && data.hardgym$getSquatPr() > 0) {
            stored = StrengthRepTable.migrateOldPr(data.hardgym$getSquatPr());
        }
        double effective = Math.max(progressionFloor, Math.max(stored, selectedWeightKg));
        if (effective > data.hardgym$getSquatEstimated1Rm()) data.hardgym$setSquatEstimated1Rm(effective);
        return effective;
    }

    private static int holdingStaminaDrain(int weightKg, int anim) {
        int base = weightKg <= 80 ? 1 : (weightKg <= 140 ? 2 : 3);
        if (anim == 2) return base + 5;
        if (anim == 1) return base + 2;
        return base;
    }

    private static float holdingHungerExhaustion(int weightKg, int anim) {
        float base = 0.10F + weightKg / 1500.0F;
        if (anim == 2) return base * 1.70F;
        if (anim == 1) return base * 1.30F;
        return base;
    }

    private static float squatRepHungerExhaustion(int weightKg) {
        return 1.30F + weightKg / 80.0F;
    }

    private static float crushDamage(int weightKg) {
        if (weightKg <= 80) return 1.0F;
        if (weightKg <= 120) return 2.0F;
        if (weightKg <= 160) return 3.0F;
        return 4.0F;
    }

    private static int pinnedFatigueInstant(int weightKg) {
        // Immediate failure penalty: scales with the bar, about 9–15 FAT across squat weights.
        return Math.min(18, 8 + weightKg / 40);
    }

    private static int pinnedFatiguePerSecond(int weightKg) {
        // Rapid accumulation while the bar is still on the player: about +2 to +5 FAT/sec.
        return Math.min(6, 2 + weightKg / 100);
    }

    private static int strengthXp(int weightKg) { return Math.max(8, weightKg / 3); }
    private static int legsXp(int weightKg) { return Math.max(12, weightKg / 2); }

    private static int ticksPerRep(ServerPlayerEntity player, int weightKg) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int legsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
        int required = requiredStrength(weightKg);
        int extraStrength = Math.max(0, strengthLevel - required);
        int extraLegs = Math.max(0, legsLevel - 1);
        int base = 44 + weightKg / 4;
        return Math.max(24, base - extraStrength * 2 - extraLegs);
    }

    private static void playUnrackSound(SquatSession session) {
        float pitch = weightPitch(session.weightKg) + 0.06F;
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.80F, pitch);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.50F, pitch);
    }

    private static void playRackSound(SquatSession session) {
        float pitch = weightPitch(session.weightKg);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.90F, pitch);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.65F, pitch - 0.04F);
    }

    private static void playDroppedBarSound(SquatSession session) {
        float pitch = Math.max(0.52F, weightPitch(session.weightKg) - 0.15F);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_FALL, SoundCategory.BLOCKS, 1.0F, pitch);
    }

    private static void playMovementSound(SquatSession session, int oldAnim, int newAnim, int progress) {
        float pitch = weightPitch(session.weightKg);
        if (oldAnim == 0 && newAnim == 1 && progress < 50) {
            session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.BLOCKS, 0.34F, pitch - 0.08F);
            return;
        }
        if (oldAnim == 2 && newAnim == 1 && progress >= 50) {
            session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_HIT, SoundCategory.BLOCKS, 0.45F, pitch + 0.05F);
        }
    }

    private static float weightPitch(int weightKg) {
        return Math.max(0.66F, 1.06F - (weightKg - 40) * 0.0025F);
    }

    private static int animationForProgress(int progressPercent) {
        if (progressPercent < 20) return 0;
        if (progressPercent < 45) return 1;
        if (progressPercent < 70) return 2;
        if (progressPercent < 90) return 1;
        return 0;
    }

    private static void setAnimation(ServerWorld world, BlockPos pos, int anim) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != HardGymMod.SQUAT_RACK) return;
        if (state.get(SquatRackBlock.ANIM) != anim) {
            world.setBlockState(pos, state.with(SquatRackBlock.ANIM, anim), 3);
        }
    }

    private static void setActive(ServerWorld world, BlockPos pos, boolean active) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != HardGymMod.SQUAT_RACK) return;
        if (state.get(SquatRackBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(SquatRackBlock.ACTIVE, active), 3);
        }
    }

    private static final class SquatSession {
        private final UUID uuid;
        private final ServerWorld world;
        private final BlockPos pos;
        private final int weightKg;
        private final double startX, startY, startZ;
        private final float startYaw, startPitch;
        private int ticksPerRep, ticksRemaining, reps, lastAnim;
        private final int targetReps;
        private boolean pressing, pinned;
        private int holdDrainTicker, damageTicker, hudTicker;

        private SquatSession(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int weightKg, int ticksPerRep, int targetReps) {
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
