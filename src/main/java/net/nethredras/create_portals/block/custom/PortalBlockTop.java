package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;

public class PortalBlockTop extends AbstractPortalBlock {

    public PortalBlockTop(Properties properties) {
        super(properties);
    }

    public PortalBlockEntity getPortalData(Level level, BlockPos pos) {
        return (PortalBlockEntity) level.getBlockEntity(pos.below());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        // Vertical linkage: the bottom half must exist directly below me
        if (direction == Direction.DOWN) {
            boolean bottomStillThere = neighborState.is(ModBlocks.PORTAL_BLOCK_BOTTOM.get());
            if (!bottomStillThere) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        // Horizontal linkage: wall behind me must still be valid
        if (!wallBehindStillValid(state, direction, neighborState, level, neighborPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }


}
