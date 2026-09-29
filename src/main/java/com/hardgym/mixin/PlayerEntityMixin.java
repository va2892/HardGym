package com.hardgym.mixin;

import com.hardgym.player.GymPlayerData;
import com.hardgym.player.GymStats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements GymPlayerData {
    @Unique private int hardgym$strengthXp = 0;
    @Unique private int hardgym$strengthIdleSeconds = 0;
    @Unique private int hardgym$bicepsXp = 0;
    @Unique private int hardgym$chestXp = 0;
    @Unique private int hardgym$tricepsXp = 0;
    @Unique private int hardgym$legsXp = 0;
    @Unique private int hardgym$benchXp = 0;
    @Unique private int hardgym$squatXp = 0;
    @Unique private int hardgym$deadliftXp = 0;
    @Unique private int hardgym$stamina = 100;
    @Unique private int hardgym$fatigue = 0;
    @Unique private int hardgym$missedSleepNights = 0;
    @Unique private long hardgym$lastSleepCheckDay = -1L;
    @Unique private int hardgym$totalReps = 0;
    @Unique private int hardgym$benchPressReps = 0;
    @Unique private int hardgym$benchPressPr = 0;
    @Unique private int hardgym$squatReps = 0;
    @Unique private int hardgym$squatPr = 0;
    @Unique private int hardgym$deadliftReps = 0;
    @Unique private int hardgym$deadliftPr = 0;
    @Unique private double hardgym$benchEstimated1Rm = 0.0D;
    @Unique private double hardgym$squatEstimated1Rm = 0.0D;
    @Unique private double hardgym$deadliftEstimated1Rm = 0.0D;

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void hardgym$writeData(NbtCompound tag, CallbackInfo ci) {
        NbtCompound gym = new NbtCompound();
        gym.putInt("StrengthXp", hardgym$strengthXp);
        gym.putInt("StrengthIdleSeconds", hardgym$strengthIdleSeconds);
        gym.putInt("BicepsXp", hardgym$bicepsXp);
        gym.putInt("ChestXp", hardgym$chestXp);
        gym.putInt("TricepsXp", hardgym$tricepsXp);
        gym.putInt("LegsXp", hardgym$legsXp);
        gym.putInt("BenchXp", hardgym$benchXp);
        gym.putInt("SquatXp", hardgym$squatXp);
        gym.putInt("DeadliftXp", hardgym$deadliftXp);
        gym.putInt("Stamina", hardgym$stamina);
        gym.putInt("Fatigue", hardgym$fatigue);
        gym.putInt("MissedSleepNights", hardgym$missedSleepNights);
        gym.putLong("LastSleepCheckDay", hardgym$lastSleepCheckDay);
        gym.putInt("TotalReps", hardgym$totalReps);
        gym.putInt("BenchPressReps", hardgym$benchPressReps);
        gym.putInt("BenchPressPr", hardgym$benchPressPr);
        gym.putInt("SquatReps", hardgym$squatReps);
        gym.putInt("SquatPr", hardgym$squatPr);
        gym.putInt("DeadliftReps", hardgym$deadliftReps);
        gym.putInt("DeadliftPr", hardgym$deadliftPr);
        gym.putDouble("BenchEstimated1Rm", hardgym$benchEstimated1Rm);
        gym.putDouble("SquatEstimated1Rm", hardgym$squatEstimated1Rm);
        gym.putDouble("DeadliftEstimated1Rm", hardgym$deadliftEstimated1Rm);
        tag.put("HardGym", gym);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void hardgym$readData(NbtCompound tag, CallbackInfo ci) {
        if (!tag.contains("HardGym", 10)) {
            return;
        }

        NbtCompound gym = tag.getCompound("HardGym");
        hardgym$strengthXp = Math.max(0, gym.getInt("StrengthXp"));
        hardgym$strengthIdleSeconds = Math.max(0, gym.getInt("StrengthIdleSeconds"));
        hardgym$bicepsXp = Math.max(0, gym.getInt("BicepsXp"));
        hardgym$chestXp = Math.max(0, gym.getInt("ChestXp"));
        hardgym$tricepsXp = Math.max(0, gym.getInt("TricepsXp"));
        hardgym$legsXp = Math.max(0, gym.getInt("LegsXp"));
        hardgym$stamina = GymStats.clamp(gym.getInt("Stamina"), 0, 100);
        hardgym$fatigue = GymStats.clamp(gym.getInt("Fatigue"), 0, 100);
        hardgym$missedSleepNights = gym.contains("MissedSleepNights", 3)
                ? Math.max(0, gym.getInt("MissedSleepNights")) : 0;
        hardgym$lastSleepCheckDay = gym.contains("LastSleepCheckDay", 4)
                ? gym.getLong("LastSleepCheckDay") : -1L;
        hardgym$totalReps = Math.max(0, gym.getInt("TotalReps"));
        hardgym$benchPressReps = Math.max(0, gym.getInt("BenchPressReps"));
        hardgym$benchPressPr = Math.max(0, gym.getInt("BenchPressPr"));
        hardgym$squatReps = Math.max(0, gym.getInt("SquatReps"));
        hardgym$squatPr = Math.max(0, gym.getInt("SquatPr"));
        hardgym$deadliftReps = Math.max(0, gym.getInt("DeadliftReps"));
        hardgym$deadliftPr = Math.max(0, gym.getInt("DeadliftPr"));

        // v0.9.2 %1RM model. Old PRs had no rep count attached, so migrate them
        // as an approximate 5RM. This makes e.g. old 100 kg PR ~= 118 kg estimated 1RM.
        hardgym$benchEstimated1Rm = gym.contains("BenchEstimated1Rm", 6)
                ? Math.max(0.0D, gym.getDouble("BenchEstimated1Rm"))
                : com.hardgym.player.StrengthRepTable.migrateOldPr(hardgym$benchPressPr);
        hardgym$squatEstimated1Rm = gym.contains("SquatEstimated1Rm", 6)
                ? Math.max(0.0D, gym.getDouble("SquatEstimated1Rm"))
                : com.hardgym.player.StrengthRepTable.migrateOldPr(hardgym$squatPr);
        hardgym$deadliftEstimated1Rm = gym.contains("DeadliftEstimated1Rm", 6)
                ? Math.max(0.0D, gym.getDouble("DeadliftEstimated1Rm"))
                : com.hardgym.player.StrengthRepTable.migrateOldPr(hardgym$deadliftPr);

        // v0.9.0 exercise-specific progression. Preserve old worlds by deriving a
        // reasonable starting point from the stats/reps they already earned.
        if (gym.contains("BenchXp", 3)) {
            hardgym$benchXp = Math.max(0, gym.getInt("BenchXp"));
        } else {
            hardgym$benchXp = hardgym$chestXp;
        }

        if (gym.contains("SquatXp", 3) && gym.contains("DeadliftXp", 3)) {
            hardgym$squatXp = Math.max(0, gym.getInt("SquatXp"));
            hardgym$deadliftXp = Math.max(0, gym.getInt("DeadliftXp"));
        } else {
            int lowerReps = hardgym$squatReps + hardgym$deadliftReps;
            if (lowerReps > 0) {
                hardgym$squatXp = (int) (((long) hardgym$legsXp * hardgym$squatReps) / lowerReps);
                hardgym$deadliftXp = Math.max(0, hardgym$legsXp - hardgym$squatXp);
            } else {
                hardgym$squatXp = 0;
                hardgym$deadliftXp = 0;
            }
        }
    }

    @Override public int hardgym$getStrengthXp() { return hardgym$strengthXp; }
    @Override public void hardgym$setStrengthXp(int value) { hardgym$strengthXp = Math.max(0, value); }
    @Override public int hardgym$getStrengthIdleSeconds() { return hardgym$strengthIdleSeconds; }
    @Override public void hardgym$setStrengthIdleSeconds(int value) { hardgym$strengthIdleSeconds = Math.max(0, value); }
    @Override public int hardgym$getBicepsXp() { return hardgym$bicepsXp; }
    @Override public void hardgym$setBicepsXp(int value) { hardgym$bicepsXp = Math.max(0, value); }
    @Override public int hardgym$getChestXp() { return hardgym$chestXp; }
    @Override public void hardgym$setChestXp(int value) { hardgym$chestXp = Math.max(0, value); }
    @Override public int hardgym$getTricepsXp() { return hardgym$tricepsXp; }
    @Override public void hardgym$setTricepsXp(int value) { hardgym$tricepsXp = Math.max(0, value); }
    @Override public int hardgym$getLegsXp() { return hardgym$legsXp; }
    @Override public void hardgym$setLegsXp(int value) { hardgym$legsXp = Math.max(0, value); }
    @Override public int hardgym$getBenchXp() { return hardgym$benchXp; }
    @Override public void hardgym$setBenchXp(int value) { hardgym$benchXp = Math.max(0, value); }
    @Override public int hardgym$getSquatXp() { return hardgym$squatXp; }
    @Override public void hardgym$setSquatXp(int value) { hardgym$squatXp = Math.max(0, value); }
    @Override public int hardgym$getDeadliftXp() { return hardgym$deadliftXp; }
    @Override public void hardgym$setDeadliftXp(int value) { hardgym$deadliftXp = Math.max(0, value); }
    @Override public int hardgym$getStamina() { return hardgym$stamina; }
    @Override public void hardgym$setStamina(int value) { hardgym$stamina = GymStats.clamp(value, 0, 100); }
    @Override public int hardgym$getFatigue() { return hardgym$fatigue; }
    @Override public void hardgym$setFatigue(int value) { hardgym$fatigue = GymStats.clamp(value, 0, 100); }
    @Override public int hardgym$getMissedSleepNights() { return hardgym$missedSleepNights; }
    @Override public void hardgym$setMissedSleepNights(int value) { hardgym$missedSleepNights = Math.max(0, value); }
    @Override public long hardgym$getLastSleepCheckDay() { return hardgym$lastSleepCheckDay; }
    @Override public void hardgym$setLastSleepCheckDay(long value) { hardgym$lastSleepCheckDay = value; }
    @Override public int hardgym$getTotalReps() { return hardgym$totalReps; }
    @Override public void hardgym$setTotalReps(int value) { hardgym$totalReps = Math.max(0, value); }
    @Override public int hardgym$getBenchPressReps() { return hardgym$benchPressReps; }
    @Override public void hardgym$setBenchPressReps(int value) { hardgym$benchPressReps = Math.max(0, value); }
    @Override public int hardgym$getBenchPressPr() { return hardgym$benchPressPr; }
    @Override public void hardgym$setBenchPressPr(int value) { hardgym$benchPressPr = Math.max(0, value); }
    @Override public int hardgym$getSquatReps() { return hardgym$squatReps; }
    @Override public void hardgym$setSquatReps(int value) { hardgym$squatReps = Math.max(0, value); }
    @Override public int hardgym$getSquatPr() { return hardgym$squatPr; }
    @Override public void hardgym$setSquatPr(int value) { hardgym$squatPr = Math.max(0, value); }
    @Override public int hardgym$getDeadliftReps() { return hardgym$deadliftReps; }
    @Override public void hardgym$setDeadliftReps(int value) { hardgym$deadliftReps = Math.max(0, value); }
    @Override public int hardgym$getDeadliftPr() { return hardgym$deadliftPr; }
    @Override public void hardgym$setDeadliftPr(int value) { hardgym$deadliftPr = Math.max(0, value); }
    @Override public double hardgym$getBenchEstimated1Rm() { return hardgym$benchEstimated1Rm; }
    @Override public void hardgym$setBenchEstimated1Rm(double value) { hardgym$benchEstimated1Rm = Math.max(0.0D, value); }
    @Override public double hardgym$getSquatEstimated1Rm() { return hardgym$squatEstimated1Rm; }
    @Override public void hardgym$setSquatEstimated1Rm(double value) { hardgym$squatEstimated1Rm = Math.max(0.0D, value); }
    @Override public double hardgym$getDeadliftEstimated1Rm() { return hardgym$deadliftEstimated1Rm; }
    @Override public void hardgym$setDeadliftEstimated1Rm(double value) { hardgym$deadliftEstimated1Rm = Math.max(0.0D, value); }
}
