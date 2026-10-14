package com.hardgym.client;

import com.hardgym.block.EquipmentRackBlock;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Draw a cached box instead of iterating the many edges of the tilted rack frame. */
public final class EquipmentRackOutlineRenderer {
    private EquipmentRackOutlineRenderer() { }

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(EquipmentRackOutlineRenderer::render);
    }

    @SuppressWarnings("deprecation") // The event's vanilla line consumer is available in Fabric 0.42.
    private static boolean render(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outline) {
        BlockState state = outline.blockState();
        if (!(state.getBlock() instanceof EquipmentRackBlock)) return true;
        Box bounds = ((EquipmentRackBlock) state.getBlock()).getOutlineBounds(state);
        BlockPos pos = outline.blockPos();
        double x = pos.getX() - outline.cameraX();
        double y = pos.getY() - outline.cameraY();
        double z = pos.getZ() - outline.cameraZ();
        WorldRenderer.drawBox(context.matrixStack(), outline.vertexConsumer(),
                x + bounds.minX, y + bounds.minY, z + bounds.minZ,
                x + bounds.maxX, y + bounds.maxY, z + bounds.maxZ, 0, 0, 0, 0.4F);
        return false;
    }
}
