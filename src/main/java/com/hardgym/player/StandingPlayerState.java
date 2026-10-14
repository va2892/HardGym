package com.hardgym.player;

import net.minecraft.util.math.BlockPos;

/** Temporary squat/deadlift ownership, synchronized with the player's entity data. */
public interface StandingPlayerState {
    BlockPos hardgym$getSquatPos();

    void hardgym$setSquatPos(BlockPos pos);

    BlockPos hardgym$getDeadliftPos();

    void hardgym$setDeadliftPos(BlockPos pos);
}
