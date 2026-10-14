package com.hardgym.client;

import com.hardgym.block.BenchPressBlock;
import com.hardgym.player.BenchPlayerState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public final class BenchClientState {
    private BenchClientState() {
    }

    public static BenchMatch findActiveBench(LivingEntity entity) {
        if (!(entity instanceof BenchPlayerState) || !entity.isAlive()) {
            return null;
        }

        // Exact ownership survives remote interpolation and all three bar phases.
        // Nearby players must not enter or lose the pose because of a distance test.
        BlockPos pos = ((BenchPlayerState)entity).hardgym$getBenchPos();
        if (pos == null) {
            return null;
        }
        BlockState state = entity.world.getBlockState(pos);
        if (state.getBlock() instanceof BenchPressBlock && state.get(BenchPressBlock.ACTIVE)) {
            return new BenchMatch(pos, state);
        }
        return null;
    }

    public static final class BenchMatch {
        public final BlockPos pos;
        public final BlockState state;

        public BenchMatch(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
