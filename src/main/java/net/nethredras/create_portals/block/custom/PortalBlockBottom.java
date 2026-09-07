package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;

public class PortalBlockBottom extends AbstractPortalBlock implements EntityBlock {

    public PortalBlockBottom(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    public PortalBlockEntity getPortalData(Level level, BlockPos pos) {
        return (PortalBlockEntity) level.getBlockEntity(pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        // Vertical linkage: the top half must exist directly above me
        if (direction == Direction.UP) {
            boolean topStillThere = neighborState.is(ModBlocks.PORTAL_BLOCK_TOP.get());
            if (!topStillThere) {
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
