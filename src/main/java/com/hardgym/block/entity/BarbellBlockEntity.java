package com.hardgym.block.entity;

import com.hardgym.HardGymMod;
import com.hardgym.animation.ExerciseAnimation;
import com.hardgym.block.BarbellStationBlock;
import com.hardgym.player.BarbellLoad;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BarbellBlockEntity extends BlockEntity implements BlockEntityClientSerializable, Tickable {
    private BarbellLoad load;
    private float exercisePhase;
    private boolean hasExercisePhase;
    private float previousVisualPhase, visualPhase;
    private boolean visualInitialized;

    public BarbellBlockEntity() {
        super(HardGymMod.BARBELL_BLOCK_ENTITY);
    }

    private static int capacity(BlockState state) {
        return state.getBlock() instanceof BarbellStationBlock ? ((BarbellStationBlock) state.getBlock()).getPairCapacity() : 9;
    }

    private BarbellLoad load() {
        if (load == null) load = new BarbellLoad(capacity(getCachedState()));
        return load;
    }

    public int getWeightKg() {
        return load().getWeightKg();
    }

    public int getPairCount() {
        return load().size();
    }

    public int[] getPairs() {
        return load().getPairs();
    }

    public static void setExercisePhase(World world, BlockPos pos, float phase) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof BarbellBlockEntity) ((BarbellBlockEntity) entity).setExercisePhase(phase);
    }

    private void setExercisePhase(float phase) {
        phase = ExerciseAnimation.clamp(phase, 0.0F, 2.0F);
        if (hasExercisePhase && exercisePhase == phase) return;
        exercisePhase = phase;
        hasExercisePhase = true;
        if (world != null && !world.isClient) sync();
    }

    private float targetPhase() {
        return hasExercisePhase ? exercisePhase : getCachedState().get(BarbellStationBlock.ANIM);
    }

    public float getExercisePhase(float tickDelta) {
        if (!visualInitialized) return targetPhase();
        return previousVisualPhase + (visualPhase - previousVisualPhase) * ExerciseAnimation.clamp(tickDelta, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        if (world == null || !world.isClient) return;
        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof BarbellStationBlock) || !state.get(BarbellStationBlock.ACTIVE)) {
            visualInitialized = false;
            return;
        }
        if (!visualInitialized) {
            previousVisualPhase = visualPhase = targetPhase();
            visualInitialized = true;
            return;
        }
        previousVisualPhase = visualPhase;
        float distance = targetPhase() - visualPhase;
        float step = ExerciseAnimation.clamp(distance * 0.65F, -0.35F, 0.35F);
        visualPhase += step;
        if (Math.abs(distance) < 0.00001F) visualPhase = targetPhase();
    }

    public boolean addPair(int weightKg) {
        if (!load().addPair(weightKg)) return false;
        changed();
        return true;
    }

    public int removeLastPair() {
        int removed = load().removeLast();
        if (removed > 0) changed();
        return removed;
    }

    public int[] removeAllPairs() {
        int[] removed = load().removeAll();
        if (removed.length > 0) changed();
        return removed;
    }

    private void changed() {
        markDirty();
        if (world != null && !world.isClient) sync();
    }

    @Override
    public void fromTag(BlockState state, NbtCompound tag) {
        super.fromTag(state, tag);
        load = BarbellLoad.fromSavedPairs(capacity(state), tag.getIntArray("PlatePairs"));
        hasExercisePhase = tag.contains("ExercisePhase");
        exercisePhase = ExerciseAnimation.clamp(tag.getFloat("ExercisePhase"), 0.0F, 2.0F);
        visualInitialized = false;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putIntArray("PlatePairs", getPairs());
        return tag;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return toClientTag(writeNbt(new NbtCompound()));
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        tag.putIntArray("PlatePairs", getPairs());
        tag.putFloat("ExercisePhase", targetPhase());
        return tag;
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        load = BarbellLoad.fromSavedPairs(capacity(getCachedState()), tag.getIntArray("PlatePairs"));
        hasExercisePhase = tag.contains("ExercisePhase");
        exercisePhase = ExerciseAnimation.clamp(tag.getFloat("ExercisePhase"), 0.0F, 2.0F);
    }
}
