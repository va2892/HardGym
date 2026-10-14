package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.block.SquatRackBlock;
import com.hardgym.player.StandingPlayerState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public final class SquatClientState {
    private SquatClientState() {
    }

    public static SquatMatch findActiveSquat(LivingEntity entity) {
        if (!(entity instanceof StandingPlayerState) || !entity.isAlive()) {
            return null;
        }
        // Proximity never grants ownership, even when players overlap at the rack.
        BlockPos pos = ((StandingPlayerState)entity).hardgym$getSquatPos();
        if (pos == null) return null;
        BlockState state = entity.world.getBlockState(pos);
        return state.getBlock() == HardGymMod.SQUAT_RACK && state.get(SquatRackBlock.ACTIVE)
                ? new SquatMatch(pos, state) : null;
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
