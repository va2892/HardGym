package com.hardgym.block;

import com.hardgym.block.entity.EquipmentRackBlockEntity;
import com.hardgym.item.DumbbellItem;
import com.hardgym.item.PlatePairItem;
import com.hardgym.training.BenchPressManager;
import com.hardgym.training.DeadliftManager;
import com.hardgym.training.SquatManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** A visible, shared equipment inventory. Empty-hand clicks select a shelf or peg. */
public final class EquipmentRackBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final BooleanProperty LEFT = BooleanProperty.of("left");
    public static final BooleanProperty RIGHT = BooleanProperty.of("right");
    private final boolean plateRack;
    private final Map<Direction, VoxelShape>[] collisionShapes;
    private final Map<Direction, VoxelShape>[] outlineShapes;
    private final Map<Direction, Box>[] outlineBounds;

    @SuppressWarnings("unchecked")
    public EquipmentRackBlock(Settings settings, boolean plateRack) {
        super(settings);
        this.plateRack = plateRack;
        collisionShapes = new Map[plateRack ? 1 : 4];
        outlineShapes = new Map[collisionShapes.length];
        outlineBounds = new Map[collisionShapes.length];
        for (int connections = 0; connections < collisionShapes.length; connections++) {
            VoxelShape frame = VoxelShapes.empty();
            for (float[] piece : EquipmentRackLayout.frameBounds(plateRack, connections)) {
                frame = VoxelShapes.union(frame, Block.createCuboidShape(piece[0], piece[1], piece[2],
                        piece[3], piece[4], piece[5]));
            }
            collisionShapes[connections] = HorizontalShapeRotation.fromNorth(frame);
            VoxelShape outline = frame;
            for (float[] piece : EquipmentRackLayout.selectionBounds(plateRack)) {
                outline = VoxelShapes.union(outline, Block.createCuboidShape(piece[0], piece[1], piece[2],
                        piece[3], piece[4], piece[5]));
            }
            outlineShapes[connections] = HorizontalShapeRotation.fromNorth(outline);
            // Keep precise ray targeting, but never traverse this mesh to draw its edges.
            Map<Direction, Box> bounds = new EnumMap<>(Direction.class);
            for (Map.Entry<Direction, VoxelShape> entry : outlineShapes[connections].entrySet()) {
                bounds.put(entry.getKey(), entry.getValue().getBoundingBox());
            }
            outlineBounds[connections] = Collections.unmodifiableMap(bounds);
        }
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH)
                .with(LEFT, false).with(RIGHT, false));
    }

    public boolean isPlateRack() {
        return plateRack;
    }

    public boolean accepts(Item item) {
        return plateRack ? item instanceof PlatePairItem : item instanceof DumbbellItem;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return withConnections(getDefaultState().with(FACING, context.getPlayerFacing().getOpposite()),
                context.getWorld(), context.getBlockPos());
    }

    public static boolean connects(BlockState a, BlockState b) {
        return a != null && b != null && a.getBlock() instanceof EquipmentRackBlock
                && !((EquipmentRackBlock) a.getBlock()).isPlateRack()
                && b.getBlock() == a.getBlock() && a.get(FACING) == b.get(FACING);
    }

    public BlockState withConnections(BlockState state, WorldAccess world, BlockPos pos) {
        Direction right = state.get(FACING).rotateYClockwise();
        return state.with(LEFT, connectedAt(state, world, pos.offset(right.getOpposite())))
                .with(RIGHT, connectedAt(state, world, pos.offset(right)));
    }

    private boolean connectedAt(BlockState state, WorldAccess world, BlockPos pos) {
        return !plateRack && world.isChunkLoaded(pos) && connects(state, world.getBlockState(pos));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction right = state.get(FACING).rotateYClockwise();
        if (direction == right) return state.with(RIGHT, connects(state, neighbor));
        if (direction == right.getOpposite()) return state.with(LEFT, connects(state, neighbor));
        return state;
    }

    private int connections(BlockState state) {
        return plateRack ? 0 : (state.get(LEFT) ? 1 : 0) | (state.get(RIGHT) ? 2 : 0);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new EquipmentRackBlockEntity();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        return interact(world, pos, player, hand, hit.getPos());
    }

    public ActionResult interact(World world, BlockPos pos, PlayerEntity player, Hand hand, Vec3d hitPos) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (BenchPressManager.isTraining(player.getUuid()) || SquatManager.isTraining(player.getUuid())
                || DeadliftManager.isTraining(player.getUuid())) return ActionResult.SUCCESS;
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof EquipmentRackBlockEntity)) return ActionResult.FAIL;
        EquipmentRackBlockEntity rack = (EquipmentRackBlockEntity) entity;
        ItemStack held = player.getStackInHand(hand);
        if (hand == Hand.MAIN_HAND && held.isEmpty() && !player.isSneaking()
                && accepts(player.getStackInHand(Hand.OFF_HAND).getItem())) {
            held = player.getStackInHand(Hand.OFF_HAND);
        }

        int slot = selectedSlot(world.getBlockState(pos), pos, hitPos);
        if (!held.isEmpty()) {
            if (!accepts(held.getItem())) {
                player.sendMessage(new TranslatableText(plateRack ? "message.hardgym.rack_plates_only"
                        : "message.hardgym.rack_dumbbells_only").formatted(Formatting.YELLOW), true);
                return ActionResult.SUCCESS;
            }
            int inserted = rack.insert(held, slot, player.isSneaking() ? held.getCount() : 1);
            if (inserted == 0) {
                player.sendMessage(new TranslatableText("message.hardgym.rack_full")
                        .formatted(Formatting.YELLOW), true);
            } else {
                if (player.isUsingItem() && player.getActiveItem().getItem() instanceof DumbbellItem) {
                    player.stopUsingItem();
                }
                player.sendMessage(new TranslatableText("message.hardgym.rack_stored", inserted,
                        plateRack ? rack.storedCount() : rack.sectionOccupiedSlots(),
                        plateRack ? 40 : rack.sectionCapacity()).formatted(Formatting.GREEN), true);
            }
        } else if (player.isSneaking()) {
            int count = 0;
            for (ItemStack stack : rack.removeSection()) {
                count += stack.getCount(); returnToPlayer(player, stack);
            }
            player.sendMessage(new TranslatableText("message.hardgym.rack_taken_all", count)
                    .formatted(Formatting.GRAY), true);
        } else {
            ItemStack taken = rack.removeStack(slot, 1);
            if (taken.isEmpty()) {
                player.sendMessage(new TranslatableText("message.hardgym.rack_empty_place")
                        .formatted(Formatting.YELLOW), true);
            } else {
                player.sendMessage(new TranslatableText("message.hardgym.rack_taken", taken.getName())
                        .formatted(Formatting.GRAY), true);
                returnToPlayer(player, taken);
            }
        }
        return ActionResult.SUCCESS;
    }

    /** Inverse of the blockstate's clockwise model rotation; all coordinates are model pixels. */
    public int selectedSlot(BlockState state, BlockPos pos, Vec3d hitPos) {
        double x = hitPos.x - pos.getX();
        double z = hitPos.z - pos.getZ();
        Direction facing = state.get(FACING);
        double localX = facing == Direction.EAST ? z : facing == Direction.SOUTH ? 1.0D - x
                : facing == Direction.WEST ? 1.0D - z : x;
        double localZ = facing == Direction.EAST ? 1.0D - x : facing == Direction.SOUTH ? 1.0D - z
                : facing == Direction.WEST ? x : z;
        return EquipmentRackLayout.nearestSlot(plateRack, localX * 16.0D,
                (hitPos.y - pos.getY()) * 16.0D, localZ * 16.0D);
    }

    private static void returnToPlayer(PlayerEntity player, ItemStack stack) {
        player.inventory.insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState next, boolean moved) {
        if (state.getBlock() != next.getBlock() && !world.isClient) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof EquipmentRackBlockEntity) {
                for (ItemStack stack : ((EquipmentRackBlockEntity) entity).removeAll()) {
                    Block.dropStack(world, pos, stack);
                }
            }
        }
        super.onStateReplaced(state, world, pos, next, moved);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING))).with(LEFT, false).with(RIGHT, false);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return outlineShapes[connections(state)].get(state.get(FACING));
    }

    /** Cached envelope for the client contour and bounded destruction effect. */
    public Box getOutlineBounds(BlockState state) {
        return outlineBounds[connections(state)].get(state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return collisionShapes[connections(state)].get(state.get(FACING));
    }
}
