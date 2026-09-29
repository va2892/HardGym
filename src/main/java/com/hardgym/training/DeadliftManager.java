package com.hardgym.training;

import com.hardgym.HardGymMod;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.player.GymHud;
import com.hardgym.player.GymPlayerData;
import com.hardgym.player.GymStats;
import com.hardgym.player.GymPhysicalStats;
import com.hardgym.player.StrengthDecay;
import com.hardgym.player.StrengthRepTable;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
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

/** Manual deadlift exercise: hold RMB to pull, release to lower. */
public final class DeadliftManager {
    private static final Map<UUID, DeadliftSession> SESSIONS = new HashMap<>();
    private static final double STAND_OFFSET = 0.58D;
    private static final int[] TRAINING_WEIGHTS = new int[]{40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 220, 240, 260, 280, 300};

    private DeadliftManager() { }

    public static boolean isTraining(UUID uuid) {
        return SESSIONS.containsKey(uuid);
    }

    public static void setPressing(ServerPlayerEntity player, boolean pressing) {
        DeadliftSession session = SESSIONS.get(player.getUuid());
        if (session != null) session.pressing = pressing;
    }

    public static void clear(UUID uuid) {
        DeadliftSession session = SESSIONS.remove(uuid);
        if (session != null) {
            setAnimation(session.world, session.pos, 0);
            setActive(session.world, session.pos, false);
        }
    }

    public static void toggle(ServerPlayerEntity player, BlockPos pos, int weightKg) {
        UUID uuid = player.getUuid();
        DeadliftSession current = SESSIONS.get(uuid);
        if (current != null) {
            stop(player, current, "Подход становой закончен: " + current.reps + " повт. × " + current.weightKg + " кг");
            return;
        }

        if (BenchPressManager.isTraining(uuid) || SquatManager.isTraining(uuid)) {
            player.sendMessage(new LiteralText("Сначала закончи текущее упражнение.").formatted(Formatting.RED), true);
            return;
        }

        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int required = requiredStrength(weightKg);
        if (strengthLevel < required) {
            player.sendMessage(new LiteralText("Слишком тяжело для становой. Нужен Strength " + required + ".")
                    .formatted(Formatting.RED), true);
            return;
        }

        double oneRepMax = effectiveOneRepMax(data, strengthLevel, weightKg);
        int targetReps = StrengthRepTable.targetRepsWithFatigue(weightKg, oneRepMax, data.hardgym$getFatigue());
        int staminaCost = StrengthRepTable.staminaCostForTarget(targetReps);
        if (data.hardgym$getStamina() < staminaCost || data.hardgym$getFatigue() >= 95) {
            player.sendMessage(new LiteralText("Ты слишком устал для становой. Отдохни.")
                    .formatted(Formatting.RED), true);
            return;
        }

        DeadliftSession session = new DeadliftSession(player, player.getServerWorld(), pos, weightKg, targetReps);
        SESSIONS.put(uuid, session);
        setAnimation(session.world, pos, 0);
        setActive(session.world, pos, true);
        broadcastDeadliftState(player.getUuid(), session, true);
        lockPlayer(player, session);
        playGripSound(session);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, DeadliftSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DeadliftSession> entry = iterator.next();
            DeadliftSession session = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null || !player.isAlive()) {
                broadcastDeadliftState(entry.getKey(), session, false);
                resetBar(session);
                if (player != null) GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            if (player.isSneaking()) {
                playDropSound(session, 0.70F);
                releasePlayer(player, session);
                broadcastDeadliftState(player.getUuid(), session, false);
                resetBar(session);
                player.sendMessage(new LiteralText("Подход становой закончен: " + session.reps + " повт. × " + session.weightKg + " кг")
                        .formatted(Formatting.GRAY), true);
                GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            if (player.getServerWorld() != session.world
                    || session.world.getBlockState(session.pos).getBlock() != HardGymMod.DEADLIFT_BAR) {
                releasePlayer(player, session);
                broadcastDeadliftState(player.getUuid(), session, false);
                player.sendMessage(new LiteralText("Подход остановлен: штанга недоступна.")
                        .formatted(Formatting.GRAY), true);
                resetBar(session);
                GymHud.showRecoveryOrHide(player);
                iterator.remove();
                continue;
            }

            lockPlayer(player, session);
            GymPlayerData data = (GymPlayerData) player;

            boolean exerting = !session.returning && (session.pressing || session.progress > 0.5F);
            if (exerting && session.pressing) {
                session.staminaDrainTicker++;
                if (session.staminaDrainTicker >= 20) {
                    session.staminaDrainTicker = 0;
                    int phase = animationForProgress(Math.round(session.progress));
                    // Dynamic pulling stamina is accounted for per completed rep from %1RM.
                    // Keep the hunger cost during the pull, but do not double-charge stamina here.
                    player.getHungerManager().addExhaustion(holdingHungerExhaustion(session.weightKg, phase));
                }
            } else if (!session.pressing) {
                session.staminaDrainTicker = 0;
            }

            // Deadlift failure: no damage. The lifter simply loses the pull and drops the bar to the floor.
            if (!session.returning && session.progress > 0.5F && data.hardgym$getStamina() <= 0) {
                failAndDrop(player, session);
                iterator.remove();
                continue;
            }

            int oldAnim = animationForProgress(Math.round(session.progress));

            if (session.returning) {
                session.progress -= 100.0F / 24.0F;
                if (session.progress <= 0.0F) {
                    session.progress = 0.0F;
                    session.returning = false;
                    playFloorTouchSound(session);
                }
            } else if (session.pressing) {
                session.progress += pullSpeedPerTick(player, session.weightKg);
                if (session.progress >= 100.0F) {
                    session.progress = 100.0F;
                    setAnimation(session.world, session.pos, 2);
                    session.lastAnim = 2;
                    playLockoutSound(session);
                    if (!performRep(player, session)) {
                        failAndDrop(player, session);
                        iterator.remove();
                        continue;
                    }
                    session.returning = true;
                }
            } else if (session.progress > 0.0F) {
                // Releasing RMB does not freeze a deadlift in mid-air: gravity wins and it lowers back down.
                session.progress -= 100.0F / 30.0F;
                if (session.progress <= 0.0F) {
                    session.progress = 0.0F;
                    playFloorTouchSound(session);
                }
            }

            int anim = animationForProgress(Math.round(session.progress));
            if (anim != oldAnim || anim != session.lastAnim) {
                playMovementSound(session, session.lastAnim, anim);
                setAnimation(session.world, session.pos, anim);
                session.lastAnim = anim;
            }

            session.hudTicker++;
            if (session.hudTicker >= 4) {
                session.hudTicker = 0;
                int progress = Math.max(0, Math.min(100, Math.round(session.progress)));
                if (session.pressing && !session.returning) {
                    GymHud.showDeadlift(player, session.weightKg, session.reps,
                            data.hardgym$getStamina(), data.hardgym$getFatigue(), progress);
                } else {
                    GymHud.showDeadliftWaiting(player, session.weightKg, session.reps,
                            data.hardgym$getStamina(), data.hardgym$getFatigue(), progress);
                }
            }
        }
    }

    public static void stopForPlayer(ServerPlayerEntity player) {
        DeadliftSession current = SESSIONS.remove(player.getUuid());
        if (current != null) {
            playDropSound(current, 0.65F);
            releasePlayer(player, current);
            broadcastDeadliftState(player.getUuid(), current, false);
            resetBar(current);
            player.sendMessage(new LiteralText("Подход становой закончен: " + current.reps + " повт. × " + current.weightKg + " кг")
                    .formatted(Formatting.GRAY), true);
            GymHud.showRecoveryOrHide(player);
        }
    }

    private static void stop(ServerPlayerEntity player, DeadliftSession session, String message) {
        SESSIONS.remove(player.getUuid());
        playDropSound(session, 0.65F);
        releasePlayer(player, session);
        broadcastDeadliftState(player.getUuid(), session, false);
        resetBar(session);
        player.sendMessage(new LiteralText(message).formatted(Formatting.GRAY), true);
        GymHud.showRecoveryOrHide(player);
    }

    private static void failAndDrop(ServerPlayerEntity player, DeadliftSession session) {
        session.pressing = false;
        session.returning = false;
        session.progress = 0.0F;
        playDropSound(session, 1.0F);
        releasePlayer(player, session);
        broadcastDeadliftState(player.getUuid(), session, false);
        resetBar(session);
        player.sendMessage(new LiteralText("СИЛ НЕ ХВАТИЛО — ШТАНГА УПАЛА НА ПОЛ.")
                .formatted(Formatting.RED), false);
        GymHud.showRecoveryOrHide(player);
        // Intentionally no HP damage for deadlift failure.
    }

    private static void broadcastDeadliftState(UUID lifterUuid, DeadliftSession session, boolean active) {
        if (session == null || session.world == null || session.world.getServer() == null) return;

        for (ServerPlayerEntity receiver : session.world.getServer().getPlayerManager().getPlayerList()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(lifterUuid);
            buf.writeBoolean(active);
            if (active) {
                buf.writeBlockPos(session.pos);
            }
            ServerPlayNetworking.send(receiver, HardGymMod.DEADLIFT_STATE_PACKET, buf);
        }
    }

    private static void resetBar(DeadliftSession session) {
        setAnimation(session.world, session.pos, 0);
        setActive(session.world, session.pos, false);
    }

    private static void lockPlayer(ServerPlayerEntity player, DeadliftSession session) {
        BlockState state = session.world.getBlockState(session.pos);
        if (state.getBlock() != HardGymMod.DEADLIFT_BAR) return;
        Direction facing = DeadliftBarBlock.getFacing(state);
        float yaw = facing.asRotation();
        double x = session.pos.getX() + 0.5D - facing.getOffsetX() * STAND_OFFSET;
        double y = session.pos.getY();
        double z = session.pos.getZ() + 0.5D - facing.getOffsetZ() * STAND_OFFSET;
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
        player.networkHandler.requestTeleport(x, y, z, yaw, 0.0F);
        player.setBodyYaw(yaw);
        player.setHeadYaw(yaw);
    }

    private static void releasePlayer(ServerPlayerEntity player, DeadliftSession session) {
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.refreshPositionAndAngles(session.startX, session.startY, session.startZ, session.startYaw, session.startPitch);
        player.networkHandler.requestTeleport(session.startX, session.startY, session.startZ, session.startYaw, session.startPitch);
        player.setBodyYaw(session.startYaw);
        player.setHeadYaw(session.startYaw);
    }

    private static boolean performRep(ServerPlayerEntity player, DeadliftSession session) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int staminaCost = StrengthRepTable.staminaCostForTarget(session.targetReps);
        if (strengthLevel < requiredStrength(session.weightKg) || session.reps >= session.targetReps
                || data.hardgym$getStamina() < staminaCost) return false;

        int oldStrengthLevel = strengthLevel;
        int oldLegsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
        int oldDeadliftLevel = GymStats.levelForXp(data.hardgym$getDeadliftXp());

        data.hardgym$setStamina(data.hardgym$getStamina() - staminaCost);
        data.hardgym$setFatigue(data.hardgym$getFatigue() + StrengthRepTable.fatigueGainForRep(session.targetReps, session.reps));
        data.hardgym$setStrengthXp(data.hardgym$getStrengthXp() + strengthXp(session.weightKg));
        data.hardgym$setLegsXp(data.hardgym$getLegsXp() + legsXp(session.weightKg));
        data.hardgym$setDeadliftXp(data.hardgym$getDeadliftXp() + strengthXp(session.weightKg));
        StrengthDecay.markTraining(player);
        data.hardgym$setTotalReps(data.hardgym$getTotalReps() + 1);
        data.hardgym$setDeadliftReps(data.hardgym$getDeadliftReps() + 1);
        session.reps++;
        double setEstimate = StrengthRepTable.estimateOneRepMax(session.weightKg, session.reps);
        if (setEstimate > data.hardgym$getDeadliftEstimated1Rm()) {
            data.hardgym$setDeadliftEstimated1Rm(setEstimate);
        }
        player.getHungerManager().addExhaustion(repHungerExhaustion(session.weightKg));

        if (session.weightKg > data.hardgym$getDeadliftPr()) {
            data.hardgym$setDeadliftPr(session.weightKg);
            player.sendMessage(new LiteralText("НОВЫЙ PR В СТАНОВОЙ: " + session.weightKg + " кг!")
                    .formatted(Formatting.LIGHT_PURPLE), false);
        }

        int newStrengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int newLegsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
        int newDeadliftLevel = GymStats.levelForXp(data.hardgym$getDeadliftXp());
        if (newStrengthLevel > oldStrengthLevel) {
            player.sendMessage(new LiteralText("HardGym: Strength теперь " + newStrengthLevel + "!")
                    .formatted(Formatting.GOLD), false);
        }
        if (newLegsLevel > oldLegsLevel) {
            player.sendMessage(new LiteralText("HardGym: Legs теперь " + newLegsLevel + "!")
                    .formatted(Formatting.GREEN), false);
        }
        if (newDeadliftLevel > oldDeadliftLevel) {
            player.sendMessage(new LiteralText("HardGym: Становая Lv." + newDeadliftLevel + " — прыжок ~"
                    + GymPhysicalStats.jumpHeightPercent(newDeadliftLevel) + "% от ванильного")
                    .formatted(Formatting.AQUA), false);
        }
        return true;
    }

    public static int requiredStrength(int weightKg) {
        if (weightKg <= 50) return 1;
        if (weightKg <= 70) return 2;
        if (weightKg <= 90) return 3;
        if (weightKg <= 100) return 4;
        if (weightKg <= 120) return 5;
        if (weightKg <= 140) return 7;
        if (weightKg <= 160) return 9;
        if (weightKg <= 180) return 11;
        if (weightKg <= 200) return 14;
        if (weightKg <= 220) return 16;
        if (weightKg <= 240) return 18;
        if (weightKg <= 260) return 20;
        if (weightKg <= 280) return 22;
        return 24;
    }

    private static int holdingStaminaDrain(int weightKg, int phase) {
        int base = weightKg <= 80 ? 2 : (weightKg <= 140 ? 3 : 4);
        if (phase == 0) return base + 4; // hardest: breaking the bar from the floor
        if (phase == 1) return base + 2; // around the knees
        return base + 1;                 // near lockout
    }

    private static float holdingHungerExhaustion(int weightKg, int phase) {
        float base = 0.18F + weightKg / 1200.0F;
        if (phase == 0) return base * 1.65F;
        if (phase == 1) return base * 1.35F;
        return base;
    }

    private static double effectiveOneRepMax(GymPlayerData data, int strengthLevel, int selectedWeightKg) {
        int maxUnlocked = 0;
        for (int weight : TRAINING_WEIGHTS) {
            if (strengthLevel >= requiredStrength(weight)) maxUnlocked = weight;
        }
        double progressionFloor = maxUnlocked > 0 ? maxUnlocked / 0.85D : selectedWeightKg / 0.85D;
        double stored = data.hardgym$getDeadliftEstimated1Rm();
        if (stored <= 0.0D && data.hardgym$getDeadliftPr() > 0) {
            stored = StrengthRepTable.migrateOldPr(data.hardgym$getDeadliftPr());
        }
        double effective = Math.max(progressionFloor, Math.max(stored, selectedWeightKg));
        if (effective > data.hardgym$getDeadliftEstimated1Rm()) data.hardgym$setDeadliftEstimated1Rm(effective);
        return effective;
    }
    private static float repHungerExhaustion(int weightKg) { return 1.60F + weightKg / 70.0F; }
    private static int strengthXp(int weightKg) { return Math.max(14, weightKg / 2); }
    private static int legsXp(int weightKg) { return Math.max(10, weightKg / 3); }

    private static float pullSpeedPerTick(ServerPlayerEntity player, int weightKg) {
        GymPlayerData data = (GymPlayerData) player;
        int strengthLevel = GymStats.levelForXp(data.hardgym$getStrengthXp());
        int legsLevel = GymStats.levelForXp(data.hardgym$getLegsXp());
        int required = requiredStrength(weightKg);
        int extra = Math.max(0, strengthLevel - required) + Math.max(0, legsLevel - 1) / 2;
        int ticks = Math.max(34, 62 + weightKg / 4 - extra * 2);
        return 100.0F / ticks;
    }

    private static int animationForProgress(int progressPercent) {
        if (progressPercent < 35) return 0;
        if (progressPercent < 75) return 1;
        return 2;
    }

    private static void playGripSound(DeadliftSession session) {
        float pitch = weightPitch(session.weightKg);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.BLOCKS, 0.42F, pitch + 0.10F);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_HIT, SoundCategory.BLOCKS, 0.32F, pitch + 0.16F);
    }

    private static void playMovementSound(DeadliftSession session, int oldAnim, int newAnim) {
        if (oldAnim == newAnim) return;
        float pitch = weightPitch(session.weightKg);
        if (newAnim > oldAnim) {
            session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.BLOCKS, 0.38F, pitch);
        } else {
            session.world.playSound(null, session.pos, SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.BLOCKS, 0.30F, pitch - 0.06F);
        }
    }

    private static void playLockoutSound(DeadliftSession session) {
        float pitch = weightPitch(session.weightKg);
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_HIT, SoundCategory.BLOCKS, 0.52F, pitch + 0.04F);
    }

    private static void playFloorTouchSound(DeadliftSession session) {
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_FALL, SoundCategory.BLOCKS, 0.48F,
                Math.max(0.58F, weightPitch(session.weightKg) - 0.08F));
    }

    private static void playDropSound(DeadliftSession session, float volume) {
        session.world.playSound(null, session.pos, SoundEvents.BLOCK_METAL_FALL, SoundCategory.BLOCKS, volume,
                Math.max(0.50F, weightPitch(session.weightKg) - 0.16F));
    }

    private static float weightPitch(int weightKg) {
        return Math.max(0.62F, 1.04F - (weightKg - 40) * 0.0026F);
    }

    private static void setAnimation(ServerWorld world, BlockPos pos, int anim) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != HardGymMod.DEADLIFT_BAR) return;
        if (state.get(DeadliftBarBlock.ANIM) != anim) {
            world.setBlockState(pos, state.with(DeadliftBarBlock.ANIM, anim), 3);
        }
    }

    private static void setActive(ServerWorld world, BlockPos pos, boolean active) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != HardGymMod.DEADLIFT_BAR) return;
        if (state.get(DeadliftBarBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(DeadliftBarBlock.ACTIVE, active), 3);
        }
    }

    private static final class DeadliftSession {
        private final ServerWorld world;
        private final BlockPos pos;
        private final int weightKg;
        private final double startX, startY, startZ;
        private final float startYaw, startPitch;
        private float progress;
        private int reps;
        private final int targetReps;
        private int lastAnim;
        private boolean pressing;
        private boolean returning;
        private int staminaDrainTicker;
        private int hudTicker;

        private DeadliftSession(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int weightKg, int targetReps) {
            this.world = world;
            this.pos = pos.toImmutable();
            this.weightKg = weightKg;
            this.startX = player.getX();
            this.startY = player.getY();
            this.startZ = player.getZ();
            this.startYaw = player.yaw;
            this.startPitch = player.pitch;
            this.progress = 0.0F;
            this.reps = 0;
            this.targetReps = Math.max(1, targetReps);
            this.lastAnim = 0;
            this.pressing = false;
            this.returning = false;
            this.staminaDrainTicker = 0;
            this.hudTicker = 0;
        }
    }
}
