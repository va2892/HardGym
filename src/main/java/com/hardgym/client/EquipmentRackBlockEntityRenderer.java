package com.hardgym.client;

import com.hardgym.block.EquipmentRackBlock;
import com.hardgym.block.EquipmentRackLayout;
import com.hardgym.block.entity.EquipmentRackBlockEntity;
import com.hardgym.item.DumbbellItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;

/** Renders only stored equipment; an empty rack never creates decorative free weights. */
public final class EquipmentRackBlockEntityRenderer extends BlockEntityRenderer<EquipmentRackBlockEntity> {
    // Share Minecraft's atlas buffer across all racks and weights; do not flush one texture per peg.
    private static final RenderLayer EQUIPMENT = TexturedRenderLayers.getEntitySolid();
    private static final Identifier[] PLATE_TEXTURES = {
            new Identifier("hardgym", "block/plate_green"), new Identifier("hardgym", "block/plate_yellow"),
            new Identifier("hardgym", "block/plate_blue"), new Identifier("hardgym", "block/plate_red"),
            new Identifier("hardgym", "block/plate_black")
    };
    private final Map<BakedModel, ItemMesh> itemMeshes = new WeakHashMap<>();
    public EquipmentRackBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(EquipmentRackBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light, int overlay) {
        BlockState state = entity.getCachedState();
        if (state == null || !(state.getBlock() instanceof EquipmentRackBlock)) return;
        if (!entity.hasDisplayedEquipment()) return;
        Direction facing = state.get(EquipmentRackBlock.FACING);
        float angle = facing == Direction.EAST ? 90 : facing == Direction.SOUTH ? 180
                : facing == Direction.WEST ? 270 : 0;
        matrices.push();
        matrices.translate(0.5D, 0.0D, 0.5D);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-angle));
        matrices.translate(-0.5D, 0.0D, -0.5D);

        if (entity.isPlateRack()) {
            matrices.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            VertexConsumer vertices = consumers.getBuffer(EQUIPMENT);
            Function<Identifier, Sprite> atlas = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            for (int slot = 0; slot < entity.size(); slot++) {
                int count = entity.getStack(slot).getCount();
                if (count == 0) continue;
                RackPlateMesh.render(slot, count, matrices, vertices, atlas.apply(PLATE_TEXTURES[slot]), light, overlay);
            }
        } else {
            ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
            VertexConsumer vertices = consumers.getBuffer(EQUIPMENT);
            for (int slot = 0; slot < entity.size(); slot++) {
                ItemStack stack = entity.getDisplayedStack(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof DumbbellItem)) continue;
                int weight = ((DumbbellItem) stack.getItem()).getWeightKg();
                matrices.push();
                matrices.translate(EquipmentRackLayout.slotX(false, slot) / 16.0D,
                        EquipmentRackLayout.dumbbellCenterY(slot, weight) / 16.0D,
                        EquipmentRackLayout.dumbbellCenterZ(slot, weight) / 16.0D);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));
                float scale = EquipmentRackLayout.DUMBBELL_SCALE;
                matrices.scale(scale, scale, scale);
                BakedModel model = itemRenderer.getHeldItemModel(stack, entity.getWorld(), null);
                ItemMesh mesh = itemMeshes.get(model);
                if (mesh == null) {
                    mesh = new ItemMesh(model);
                    itemMeshes.put(model, mesh);
                }
                if (stack.hasGlint() || !mesh.fast) {
                    // Preserve enchantment glint, tint providers and custom builtin item renderers.
                    itemRenderer.renderItem(stack, ModelTransformation.Mode.NONE, false, matrices, consumers,
                            light, overlay, model);
                    // A custom renderer may have switched or flushed the shared buffer.
                    vertices = consumers.getBuffer(EQUIPMENT);
                } else {
                    // NONE leaves the item model unchanged; vanilla item rendering centers it at -0.5.
                    matrices.translate(-0.5D, -0.5D, -0.5D);
                    MatrixStack.Entry entry = matrices.peek();
                    for (BakedQuad quad : mesh.quads) vertices.quad(entry, quad, 1.0F, 1.0F, 1.0F, light, overlay);
                }
                matrices.pop();
            }
        }
        matrices.pop();
    }

    private static final class ItemMesh {
        private final BakedQuad[] quads;
        private final boolean fast;

        private ItemMesh(BakedModel model) {
            List<BakedQuad> result = new ArrayList<>();
            Random random = new Random(42L);
            if (!model.isBuiltin()) {
                for (Direction face : Direction.values()) {
                    random.setSeed(42L);
                    result.addAll(model.getQuads(null, face, random));
                }
                random.setSeed(42L);
                result.addAll(model.getQuads(null, null, random));
            }
            quads = result.toArray(new BakedQuad[0]);
            boolean plain = !model.isBuiltin();
            for (BakedQuad quad : quads) if (quad.hasColor()) plain = false;
            fast = plain;
        }
    }
    // All rack equipment fits inside its block; the default frustum check can skip off-screen racks.
}
