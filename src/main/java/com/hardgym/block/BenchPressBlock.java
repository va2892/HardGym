package com.hardgym.block;

import com.hardgym.training.BenchPressManager;
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

public class BenchPressBlock extends BarbellStationBlock {
    // Legacy property retained for existing world states; the plate inventory holds the actual load.
    public static final IntProperty WEIGHT = IntProperty.of("weight", 0, 12);

    // The bench is intentionally larger than one block now: about 2.2 blocks long.
    // One logical block still controls it, but the visual/collision shape extends out from it.
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2.0D, 0.0D, -9.0D, 14.0D, 3.0D, 25.0D),
            Block.createCuboidShape(4.0D, 3.0D, -8.0D, 12.0D, 7.0D, 21.0D),
            Block.createCuboidShape(1.0D, 3.0D, 15.0D, 4.0D, 18.0D, 21.0D),
            Block.createCuboidShape(12.0D, 3.0D, 15.0D, 15.0D, 18.0D, 21.0D),
            Block.createCuboidShape(-6.0D, 13.0D, 11.0D, 22.0D, 18.0D, 15.0D)
    );

    private static final Map<Direction, VoxelShape> SHAPES_BY_FACING =
            HorizontalShapeRotation.fromNorth(NORTH_SHAPE);

    public BenchPressBlock(Settings settings) {
        super(settings, 5);
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
                .with(FACING, ctx.getPlayerFacing().getOpposite())
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
        BenchPressManager.toggle(player, pos, weightKg);
    }
}
