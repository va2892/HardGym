package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.block.SquatRackBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public final class SquatClientState {
    private SquatClientState() {
    }

    public static SquatMatch findActiveSquat(LivingEntity entity) {
        BlockPos center = entity.getBlockPos();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = entity.world.getBlockState(pos);
                    if (state.getBlock() == HardGymMod.SQUAT_RACK
                            && state.get(SquatRackBlock.ACTIVE)
                            && isEntityAtAnchor(entity, pos)) {
                        return new SquatMatch(pos, state);
                    }
                }
            }
        }
        return null;
    }

    private static boolean isEntityAtAnchor(LivingEntity entity, BlockPos pos) {
        double anchorX = pos.getX() + 0.5D;
        double anchorY = pos.getY();
        double anchorZ = pos.getZ() + 0.5D;
        double dx = entity.getX() - anchorX;
        double dy = entity.getY() - anchorY;
        double dz = entity.getZ() - anchorZ;
        return (dx * dx + dz * dz) <= 0.55D && Math.abs(dy) <= 0.80D;
    }

    public static final class SquatMatch {
        public final BlockPos pos;
        public final BlockState state;
        public SquatMatch(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
