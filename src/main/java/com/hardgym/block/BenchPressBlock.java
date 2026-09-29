package com.hardgym.block;

import com.hardgym.training.BenchPressManager;
import com.hardgym.training.DeadliftManager;
import com.hardgym.training.SquatManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BenchPressBlock extends HorizontalFacingBlock {
    public static final IntProperty WEIGHT = IntProperty.of("weight", 0, 12);
    public static final IntProperty ANIM = IntProperty.of("anim", 0, 2);
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    private static final int[] WEIGHTS_KG = new int[]{40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 220};

    // The bench is intentionally larger than one block now: about 2.2 blocks long.
    // One logical block still controls it, but the visual/collision shape extends out from it.
    private static final VoxelShape NORTH_SOUTH_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2.0D, 0.0D, -9.0D, 14.0D, 3.0D, 25.0D),
            Block.createCuboidShape(4.0D, 3.0D, -8.0D, 12.0D, 7.0D, 21.0D),
            Block.createCuboidShape(1.0D, 3.0D, 15.0D, 4.0D, 18.0D, 21.0D),
            Block.createCuboidShape(12.0D, 3.0D, 15.0D, 15.0D, 18.0D, 21.0D),
            Block.createCuboidShape(-6.0D, 13.0D, 11.0D, 22.0D, 18.0D, 15.0D)
    );

    private static final VoxelShape EAST_WEST_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(-9.0D, 0.0D, 2.0D, 25.0D, 3.0D, 14.0D),
            Block.createCuboidShape(-8.0D, 3.0D, 4.0D, 21.0D, 7.0D, 12.0D),
            Block.createCuboidShape(15.0D, 3.0D, 1.0D, 21.0D, 18.0D, 4.0D),
            Block.createCuboidShape(15.0D, 3.0D, 12.0D, 21.0D, 18.0D, 15.0D),
            Block.createCuboidShape(11.0D, 13.0D, -6.0D, 15.0D, 18.0D, 22.0D)
    );

    public BenchPressBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(WEIGHT, 2)
                .with(ANIM, 0)
                .with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, WEIGHT, ANIM, ACTIVE);
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
        return state.get(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        int index = state.get(WEIGHT);

        if (BenchPressManager.isTraining(player.getUuid()) || SquatManager.isTraining(player.getUuid())
                || DeadliftManager.isTraining(player.getUuid())) {
            return ActionResult.SUCCESS;
        }

        if (state.get(ACTIVE)) {
            player.sendMessage(new LiteralText("Скамья занята.")
                    .formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }

        if (player.isSneaking()) {
            if (state.get(ACTIVE)) {
                player.sendMessage(new LiteralText("Сначала закончи подход.")
                        .formatted(Formatting.RED), true);
                return ActionResult.SUCCESS;
            }

            int next = (index + 1) % WEIGHTS_KG.length;
            world.setBlockState(pos, state.with(WEIGHT, next).with(ANIM, 0), 3);
            player.sendMessage(new LiteralText("Вес на скамье: " + WEIGHTS_KG[next] + " кг")
                    .formatted(Formatting.YELLOW), true);
            return ActionResult.SUCCESS;
        }

        BenchPressManager.toggle((ServerPlayerEntity) player, pos, WEIGHTS_KG[index]);
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public static int getWeightKg(BlockState state) {
        return WEIGHTS_KG[state.get(WEIGHT)];
    }

    public static Direction getFacing(BlockState state) {
        return state.get(FACING);
    }
}
