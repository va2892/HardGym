package com.hardgym.block;

import com.hardgym.training.DeadliftManager;
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

public class DeadliftBarBlock extends BarbellStationBlock {
    // Legacy property retained for existing world states; the plate inventory holds the actual load.
    public static final IntProperty WEIGHT = IntProperty.of("weight", 0, 16);

    // Selection shape only. Collision is intentionally empty so the lifter can stand close to the bar.
    private static final VoxelShape NORTH_SOUTH_OUTLINE = Block.createCuboidShape(-12.0D, 0.0D, 5.0D, 28.0D, 6.0D, 11.0D);
    private static final VoxelShape EAST_WEST_OUTLINE = Block.createCuboidShape(5.0D, 0.0D, -12.0D, 11.0D, 6.0D, 28.0D);

    public DeadliftBarBlock(Settings settings) {
        super(settings, 9);
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
        return state.get(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_OUTLINE : NORTH_SOUTH_OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected void startTraining(ServerPlayerEntity player, BlockPos pos, int weightKg) {
        DeadliftManager.toggle(player, pos, weightKg);
    }
}
