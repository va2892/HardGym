package com.hardgym.client;

import com.hardgym.HardGymMod;
import com.hardgym.block.DeadliftBarBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeadliftClientState {
    private static final double STAND_OFFSET = 0.58D;

    // Server-synced ownership of an active deadlift bar.
    // This avoids relying on the remote player's interpolated position in multiplayer.
    private static final Map<UUID, BlockPos> SYNCED_DEADLIFTS = new HashMap<>();

    private DeadliftClientState() { }

    public static void setSyncedDeadlift(UUID playerUuid, BlockPos pos, boolean active) {
        if (playerUuid == null) return;
        if (active && pos != null) {
            SYNCED_DEADLIFTS.put(playerUuid, pos.toImmutable());
        } else {
            SYNCED_DEADLIFTS.remove(playerUuid);
        }
    }

    public static void clearSyncedDeadlifts() {
        SYNCED_DEADLIFTS.clear();
    }

    public static DeadliftMatch findActiveDeadlift(LivingEntity entity) {
        // Preferred path in multiplayer: exact lifter UUID -> active bar position
        BlockPos syncedPos = SYNCED_DEADLIFTS.get(entity.getUuid());
        if (syncedPos != null) {
            BlockState syncedState = entity.world.getBlockState(syncedPos);
            if (syncedState.getBlock() == HardGymMod.DEADLIFT_BAR
                    && syncedState.get(DeadliftBarBlock.ACTIVE)) {
                return new DeadliftMatch(syncedPos, syncedState);
            }
            // Stale entry after unload/stop: remove it and fall back to local detection.
            SYNCED_DEADLIFTS.remove(entity.getUuid());
        }

        // Fallback keeps singleplayer/edge cases working even if a sync packet was missed.
        BlockPos center = entity.getBlockPos();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = entity.world.getBlockState(pos);
                    if (state.getBlock() == HardGymMod.DEADLIFT_BAR
                            && state.get(DeadliftBarBlock.ACTIVE)
                            && isEntityAtAnchor(entity, pos, state)) {
                        return new DeadliftMatch(pos, state);
                    }
                }
            }
        }
        return null;
    }

    private static boolean isEntityAtAnchor(LivingEntity entity, BlockPos pos, BlockState state) {
        Direction facing = DeadliftBarBlock.getFacing(state);
        double anchorX = pos.getX() + 0.5D - facing.getOffsetX() * STAND_OFFSET;
        double anchorY = pos.getY();
        double anchorZ = pos.getZ() + 0.5D - facing.getOffsetZ() * STAND_OFFSET;
        double dx = entity.getX() - anchorX;
        double dy = entity.getY() - anchorY;
        double dz = entity.getZ() - anchorZ;
        // Slightly more tolerant than v0.8.1 for interpolation on remote clients.
        return (dx * dx + dz * dz) <= 0.80D && Math.abs(dy) <= 1.10D;
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
