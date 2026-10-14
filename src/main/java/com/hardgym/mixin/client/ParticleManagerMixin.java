package com.hardgym.mixin.client;

import com.hardgym.block.EquipmentRackBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rack debris must not multiply with every box in the selection/collision mesh. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Shadow protected ClientWorld world;

    @Shadow public abstract void addParticle(Particle particle);

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void hardgym$boundedRackDebris(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(state.getBlock() instanceof EquipmentRackBlock)) return;
        ci.cancel();
        if (world == null) return;
        Box bounds = ((EquipmentRackBlock) state.getBlock()).getOutlineBounds(state);
        // 4 x 2 x 4 = 32 fragments, independent of inventory and frame detail.
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 4; z++) {
                    double u = (x + 0.5D) / 4.0D;
                    double v = (y + 0.5D) / 2.0D;
                    double w = (z + 0.5D) / 4.0D;
                    addParticle(new BlockDustParticle(world,
                            pos.getX() + bounds.minX + u * (bounds.maxX - bounds.minX),
                            pos.getY() + bounds.minY + v * (bounds.maxY - bounds.minY),
                            pos.getZ() + bounds.minZ + w * (bounds.maxZ - bounds.minZ),
                            u - 0.5D, v - 0.5D, w - 0.5D, state).setBlockPos(pos));
                }
            }
        }
    }
}
