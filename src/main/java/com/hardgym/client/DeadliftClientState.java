package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.block.DeadliftBarBlock;
import com.hardgym.player.StandingPlayerState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public final class DeadliftClientState {
    private DeadliftClientState() { }

    public static DeadliftMatch findActiveDeadlift(LivingEntity entity) {
        if (!(entity instanceof StandingPlayerState) || !entity.isAlive()) {
            return null;
        }
        // Entity data reaches late observers too; never guess a lifter from a radius.
        BlockPos pos = ((StandingPlayerState)entity).hardgym$getDeadliftPos();
        if (pos == null) return null;
        BlockState state = entity.world.getBlockState(pos);
        return state.getBlock() == HardGymMod.DEADLIFT_BAR && state.get(DeadliftBarBlock.ACTIVE)
                ? new DeadliftMatch(pos, state) : null;
    }

    public static final class DeadliftMatch {
        public final BlockPos pos;
        public final BlockState state;

        public DeadliftMatch(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
