package com.hardgym.block;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Matches blockstate model rotations: north=0, east=90, south=180, west=270. */
final class HorizontalShapeRotation {
    private HorizontalShapeRotation() {
    }

    static Map<Direction, VoxelShape> fromNorth(VoxelShape north) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, north);
        VoxelShape east = rotateClockwise(north);
        shapes.put(Direction.EAST, east);
        VoxelShape south = rotateClockwise(east);
        shapes.put(Direction.SOUTH, south);
        shapes.put(Direction.WEST, rotateClockwise(south));
        return Collections.unmodifiableMap(shapes);
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape rotated = VoxelShapes.empty();
        for (Box box : shape.getBoundingBoxes()) {
            // Rotate around the block center (0.5, 0.5), including overhangs.
            rotated = VoxelShapes.union(rotated, VoxelShapes.cuboid(
                    1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX));
        }
        return rotated;
    }
}
