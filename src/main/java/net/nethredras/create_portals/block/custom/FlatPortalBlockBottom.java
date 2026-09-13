package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;

public class FlatPortalBlockBottom extends AbstractFlatPortalBlock implements EntityBlock {
    public FlatPortalBlockBottom(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    public PortalBlockEntity getPortalData(Level level, BlockPos pos) {
        return (PortalBlockEntity) level.getBlockEntity(pos);
    }

    // Checks if the other portalBlock is around
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        // Pair linkage: the "top" half must exist beside me, in this
        // state's ORIENTATION direction
        Direction orientation = state.getValue(ORIENTATION);
        if (direction == orientation) {
            boolean pairStillThere = neighborState.is(ModBlocks.FLAT_PORTAL_BLOCK_TOP.get());
            if (!pairStillThere) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        // Surface linkage: the floor/ceiling behind me must still be valid
        if (!surfaceBehindStillValid(state, direction, neighborState, level, neighborPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
