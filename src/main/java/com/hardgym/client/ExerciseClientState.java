package com.hardgym.client;

import com.hardgym.block.BarbellStationBlock;
import com.hardgym.block.entity.BarbellBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Every visual consumer reads the same station phase, for local and remote lifters. */
public final class ExerciseClientState {
    private ExerciseClientState() { }

    public static float phase(World world, BlockPos pos, BlockState state, float tickDelta) {
        BlockEntity entity = world.getBlockEntity(pos);
        return entity instanceof BarbellBlockEntity
                ? ((BarbellBlockEntity) entity).getExercisePhase(tickDelta)
                : state.get(BarbellStationBlock.ANIM);
    }
}
