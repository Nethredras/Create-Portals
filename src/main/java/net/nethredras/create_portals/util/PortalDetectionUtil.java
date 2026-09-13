package net.nethredras.create_portals.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nethredras.create_portals.block.custom.AbstractFlatPortalBlock;
import net.nethredras.create_portals.block.custom.AbstractPortalBlock;

public class PortalDetectionUtil {
    public Direction getPortalDirection(BlockGetter level, BlockPos pos) {
        // Air blocks get skipped
        if (level.getBlockState(pos).is(Blocks.AIR)) {
            return null;
        }

        BlockPos northPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
        BlockPos eastPos = new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
        BlockPos southPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
        BlockPos westPos = new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
        BlockPos upPos = pos.above();
        BlockPos downPos = pos.below();

        // North
        if (isAbstractPortalBlock(northPos, level, Direction.NORTH)) {
            return Direction.NORTH;
        }


        // East
        if (isAbstractPortalBlock(eastPos, level, Direction.EAST)) {
            return Direction.EAST;
        }

        // South
        if (isAbstractPortalBlock(southPos, level, Direction.SOUTH)) {
            return Direction.SOUTH;
        }

        // West
        if (isAbstractPortalBlock(westPos, level, Direction.WEST)) {
            return Direction.WEST;
        }

        // Up
        if (isAbstractPortalBlock(upPos, level, Direction.UP)) {
            return Direction.UP;
        }

        // Down
        if (isAbstractPortalBlock(downPos, level, Direction.DOWN)) {
            return Direction.DOWN;
        }

        return null;
    }

    public static boolean isAbstractPortalBlock(BlockPos pos, BlockGetter level, Direction direction) {
        BlockState sourroundignBlockState = level.getBlockState(pos);

        if (sourroundignBlockState.getBlock() instanceof AbstractPortalBlock || sourroundignBlockState.getBlock() instanceof AbstractFlatPortalBlock) {
            Direction portalDirection;
            if (sourroundignBlockState.getBlock() instanceof AbstractPortalBlock) {
                portalDirection = level.getBlockState(pos).getValue(AbstractPortalBlock.FACING);
            } else {
                portalDirection = level.getBlockState(pos).getValue(AbstractFlatPortalBlock.FACING);
            }

            if (portalDirection == direction) {
                return true;
            }
        }

        return false;
    }
}
