package com.hardgym.block;

import com.hardgym.block.entity.BarbellBlockEntity;
import com.hardgym.item.PlatePairItem;
import com.hardgym.player.BarbellLoad;
import com.hardgym.training.BenchPressManager;
import com.hardgym.training.DeadliftManager;
import com.hardgym.training.SquatManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public abstract class BarbellStationBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final IntProperty ANIM = IntProperty.of("anim", 0, 2);
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    private final int pairCapacity;

    protected BarbellStationBlock(Settings settings, int pairCapacity) {
        super(settings);
        this.pairCapacity = pairCapacity;
    }

    public final int getPairCapacity() {
        return pairCapacity;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ANIM, ACTIVE);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new BarbellBlockEntity();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        return interact(world, pos, player, hand);
    }

    public ActionResult interact(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (BenchPressManager.isTraining(player.getUuid()) || SquatManager.isTraining(player.getUuid())
                || DeadliftManager.isTraining(player.getUuid())) return ActionResult.SUCCESS;
        if (world.getBlockState(pos).get(ACTIVE)) {
            player.sendMessage(new LiteralText("Сначала закончи подход: сейчас менять вес нельзя.")
                    .formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }

        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof BarbellBlockEntity)) return ActionResult.FAIL;
        BarbellBlockEntity barbell = (BarbellBlockEntity) entity;
        ItemStack stack = player.getStackInHand(hand);
        // Vanilla tries the empty main hand first; allow loading from the off hand.
        if (hand == Hand.MAIN_HAND && stack.isEmpty() && !player.isSneaking()
                && !player.getStackInHand(Hand.OFF_HAND).isEmpty()
                && player.getStackInHand(Hand.OFF_HAND).getItem() instanceof PlatePairItem) {
            stack = player.getStackInHand(Hand.OFF_HAND);
        }
        if (!stack.isEmpty() && stack.getItem() instanceof PlatePairItem) {
            int weight = ((PlatePairItem) stack.getItem()).getPlateWeightKg();
            if (!barbell.addPair(weight)) {
                player.sendMessage(new LiteralText("На грифе нет места: максимум " + pairCapacity
                        + " блинов с каждой стороны.").formatted(Formatting.RED), true);
                return ActionResult.SUCCESS;
            }
            if (!player.abilities.creativeMode) stack.decrement(1);
            showWeight(player, barbell);
            return ActionResult.SUCCESS;
        }

        if (player.isSneaking()) {
            if (!stack.isEmpty()) {
                player.sendMessage(new LiteralText("Чтобы снять последнюю пару блинов, нажми Shift + ПКМ пустой рукой.")
                        .formatted(Formatting.YELLOW), true);
                return ActionResult.SUCCESS;
            }
            int weight = barbell.removeLastPair();
            if (weight > 0) {
                ItemStack returned = new ItemStack(PlatePairItem.forWeight(weight));
                player.inventory.insertStack(returned);
                if (!returned.isEmpty()) player.dropItem(returned, false);
            }
            showWeight(player, barbell);
            return ActionResult.SUCCESS;
        }

        startTraining((ServerPlayerEntity) player, pos, barbell.getWeightKg());
        return ActionResult.SUCCESS;
    }

    private void showWeight(PlayerEntity player, BarbellBlockEntity barbell) {
        player.sendMessage(new LiteralText("Вес штанги: " + barbell.getWeightKg()
                + " кг | Блины на сторону: " + barbell.getPairCount() + "/" + pairCapacity)
                .formatted(Formatting.YELLOW), true);
    }

    protected abstract void startTraining(ServerPlayerEntity player, BlockPos pos, int weightKg);

    public static int getWeightKg(World world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        return entity instanceof BarbellBlockEntity
                ? ((BarbellBlockEntity) entity).getWeightKg() : BarbellLoad.BAR_WEIGHT_KG;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock() && !world.isClient) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof BarbellBlockEntity) {
                for (int weight : ((BarbellBlockEntity) entity).removeAllPairs()) {
                    Block.dropStack(world, pos, new ItemStack(PlatePairItem.forWeight(weight)));
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public static Direction getFacing(BlockState state) {
        return state.get(FACING);
    }
}
