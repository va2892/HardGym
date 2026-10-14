package com.hardgym.player;

import net.minecraft.util.math.BlockPos;

/** Temporary bench ownership, synchronized by the player's vanilla data tracker. */
public interface BenchPlayerState {
    BlockPos hardgym$getBenchPos();

    void hardgym$setBenchPos(BlockPos pos);
}
