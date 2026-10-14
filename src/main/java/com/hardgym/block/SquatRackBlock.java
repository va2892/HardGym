package com.hardgym.block;

import com.hardgym.training.SquatManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.Map;

public class SquatRackBlock extends BarbellStationBlock {
    // Legacy property retained for existing world states; the plate inventory holds the actual load.
    public static final IntProperty WEIGHT = IntProperty.of("weight", 0, 16);

    // Central space is left open for the player; only the feet/uprights collide.
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 0.0D, 8.0D, 5.0D, 2.0D, 16.0D),
            Block.createCuboidShape(11.0D, 0.0D, 8.0D, 16.0D, 2.0D, 16.0D),
            Block.createCuboidShape(1.5D, 0.0D, 11.5D, 3.5D, 30.0D, 13.5D),
            Block.createCuboidShape(12.5D, 0.0D, 11.5D, 14.5D, 30.0D, 13.5D)
    );

    private static final Map<Direction, VoxelShape> SHAPES_BY_FACING =
            HorizontalShapeRotation.fromNorth(NORTH_SHAPE);

    public SquatRackBlock(Settings settings) {
        super(settings, 7);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(WEIGHT, 0)
                .with(ANIM, 0)
                .with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WEIGHT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(FACING, ctx.getPlayerFacing())
                .with(ANIM, 0)
                .with(ACTIVE, false);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES_BY_FACING.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES_BY_FACING.get(state.get(FACING));
    }

    @Override
    protected void startTraining(ServerPlayerEntity player, BlockPos pos, int weightKg) {
        SquatManager.toggle(player, pos, weightKg);
    }
}
