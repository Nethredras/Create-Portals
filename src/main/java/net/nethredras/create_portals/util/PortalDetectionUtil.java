package net.nethredras.create_portals.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

        BlockState sourroundignBlockState = level.getBlockState(northPos);

        // North
        if (sourroundignBlockState.getBlock() instanceof AbstractPortalBlock) {
            Direction portalDirection = level.getBlockState(northPos).getValue(AbstractPortalBlock.FACING);

            if (portalDirection == Direction.NORTH) {
                return Direction.NORTH;
            }
        }

        // East
        sourroundignBlockState = level.getBlockState(eastPos);
        if (sourroundignBlockState.getBlock() instanceof AbstractPortalBlock) {
            Direction portalDirection = level.getBlockState(eastPos).getValue(AbstractPortalBlock.FACING);

            if (portalDirection == Direction.EAST) {
                return Direction.EAST;
            }
        }

        // South
        sourroundignBlockState = level.getBlockState(southPos);
        if (sourroundignBlockState.getBlock() instanceof AbstractPortalBlock) {
            Direction portalDirection = level.getBlockState(southPos).getValue(AbstractPortalBlock.FACING);

            if (portalDirection == Direction.SOUTH) {
                return Direction.SOUTH;
            }
        }

        // West
        sourroundignBlockState = level.getBlockState(westPos);
        if (sourroundignBlockState.getBlock() instanceof AbstractPortalBlock) {
            Direction portalDirection = level.getBlockState(westPos).getValue(AbstractPortalBlock.FACING);

            if (portalDirection == Direction.WEST) {
                return Direction.WEST;
            }
        }

        return null;
    }
}
