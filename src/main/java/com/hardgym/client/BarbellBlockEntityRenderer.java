package com.hardgym.client;

import com.hardgym.block.BarbellStationBlock;
import com.hardgym.block.entity.BarbellBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;

/** Actual plate pairs and the bar share the lifter's continuous station phase. */
public final class BarbellBlockEntityRenderer extends BlockEntityRenderer<BarbellBlockEntity> {
    private static final RenderLayer BAR = layer("bench_bar");
    private static final RenderLayer GREEN = layer("plate_green");
    private static final RenderLayer YELLOW = layer("plate_yellow");
    private static final RenderLayer BLUE = layer("plate_blue");
    private static final RenderLayer RED = layer("plate_red");
    private static final RenderLayer BLACK = layer("plate_black");
    private static final int[][][] FACES = {
            {{0,0,0},{0,1,0},{1,1,0},{1,0,0}},
            {{0,0,1},{1,0,1},{1,1,1},{0,1,1}},
            {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
            {{1,0,0},{1,1,0},{1,1,1},{1,0,1}},
            {{0,0,0},{1,0,0},{1,0,1},{0,0,1}},
            {{0,1,1},{1,1,1},{1,1,0},{0,1,0}}
    };
    private static final float[][] NORMALS = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
    private static final float[][] UV = {{0,1},{0,0},{1,0},{1,1}};

    public BarbellBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    private static RenderLayer layer(String texture) {
        return RenderLayer.getEntityCutoutNoCull(new Identifier("hardgym", "textures/block/" + texture + ".png"));
    }

    static RenderLayer plates(int weightKg) {
        switch (weightKg) {
            case 10: return GREEN;
            case 15: return YELLOW;
            case 20: return BLUE;
            case 25: return RED;
            case 50: return BLACK;
            default: throw new IllegalArgumentException("Invalid plate weight");
        }
    }

    @Override
    public void render(BarbellBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light, int overlay) {
        BlockState state = entity.getCachedState();
        if (!(state.getBlock() instanceof BarbellStationBlock)) return;
        int capacity = ((BarbellStationBlock) state.getBlock()).getPairCapacity();
        boolean active = state.get(BarbellStationBlock.ACTIVE);
        float phase = active ? entity.getExercisePhase(tickDelta) : state.get(BarbellStationBlock.ANIM);
        Direction facing = state.get(BarbellStationBlock.FACING);
        int[] pairs = entity.getPairs();
        float angle = facing == Direction.EAST ? 90 : facing == Direction.SOUTH ? 180
                : facing == Direction.WEST ? 270 : 0;

        matrices.push();
        matrices.translate(0.5D, 0.0D, 0.5D);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-angle));
        matrices.translate(-0.5D, 0.0D, -0.5D);
        matrices.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        cuboid(matrices, consumers.getBuffer(BAR),
                BarbellGeometry.barBounds(capacity, phase, active, pairs.length), light, overlay);
        for (int i = 0; i < pairs.length; i++) {
            VertexConsumer vertices = consumers.getBuffer(plates(pairs[i]));
            cuboid(matrices, vertices, BarbellGeometry.plateBounds(capacity, i, true, phase, active, pairs.length), light, overlay);
            cuboid(matrices, vertices, BarbellGeometry.plateBounds(capacity, i, false, phase, active, pairs.length), light, overlay);
        }
        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(BarbellBlockEntity entity) {
        return true;
    }

    static void cuboid(MatrixStack matrices, VertexConsumer vertices, float[] bounds, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();
        for (int face = 0; face < FACES.length; face++) {
            float[] normal = NORMALS[face];
            for (int vertex = 0; vertex < 4; vertex++) {
                int[] point = FACES[face][vertex];
                vertices.vertex(entry.getModel(), bounds[point[0] * 3],
                                bounds[1 + point[1] * 3], bounds[2 + point[2] * 3])
                        .color(255, 255, 255, 255).texture(UV[vertex][0], UV[vertex][1])
                        .overlay(overlay).light(light)
                        .normal(entry.getNormal(), normal[0], normal[1], normal[2]).next();
            }
        }
    }
}
