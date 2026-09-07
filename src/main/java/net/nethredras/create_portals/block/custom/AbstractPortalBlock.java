package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractPortalBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 0, 0, 0);

    public AbstractPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Shared horizontal linkage: if the wall this half is stuck to is no
     * longer solid/flat on the relevant face, the half breaks.
     * Subclasses call this from their own updateShape after doing their
     * own vertical (top/bottom) linkage check.
     */
    protected boolean wallBehindStillValid(BlockState state, Direction direction, BlockState neighborState,
                                           LevelAccessor level, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);

        if (direction == facing.getOpposite()) {
            return !neighborState.isAir()
                    && Block.isFaceFull(neighborState.getCollisionShape(level, neighborPos), facing);
        }

        // Not the wall-facing direction, so this check doesn't apply here.
        return true;
    }
}