package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;


public class FlatPortalBlockTop extends AbstractFlatPortalBlock {
    public FlatPortalBlockTop(Properties properties) {
        super(properties);
    }

    public PortalBlockEntity getPortalData(Level level, BlockPos pos) {
        Direction orientation = level.getBlockState(pos).getValue(ORIENTATION);
        return (PortalBlockEntity) level.getBlockEntity(pos.relative(orientation.getOpposite()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction orientation = state.getValue(ORIENTATION);
        if (direction == orientation.getOpposite()) {
            boolean bottomStillThere = neighborState.is(ModBlocks.FLAT_PORTAL_BLOCK_BOTTOM.get());
            if (!bottomStillThere) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        if (!surfaceBehindStillValid(state, direction, neighborState, level, neighborPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
