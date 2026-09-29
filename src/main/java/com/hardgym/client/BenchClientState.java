package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.block.BenchPressBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class BenchClientState {
    private BenchClientState() {
    }

    public static BenchMatch findActiveBench(LivingEntity entity) {
        BlockPos center = entity.getBlockPos();

        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = entity.world.getBlockState(pos);
                    if (state.getBlock() == HardGymMod.BENCH_PRESS_STATION
                            && state.get(BenchPressBlock.ACTIVE)
                            && isEntityAtBenchAnchor(entity, pos, state)) {
                        return new BenchMatch(pos, state);
                    }
                }
            }
        }

        return null;
    }

    private static boolean isEntityAtBenchAnchor(LivingEntity entity, BlockPos pos, BlockState state) {
        Direction headDirection = BenchPressBlock.getFacing(state);
        double anchorX = pos.getX() + 0.5D + headDirection.getOffsetX() * 0.80D;
        double anchorY = pos.getY() + 0.34D;
        double anchorZ = pos.getZ() + 0.5D + headDirection.getOffsetZ() * 0.80D;

        double dx = entity.getX() - anchorX;
        double dy = entity.getY() - anchorY;
        double dz = entity.getZ() - anchorZ;

        return (dx * dx + dz * dz) <= 0.50D && Math.abs(dy) <= 0.75D;
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
