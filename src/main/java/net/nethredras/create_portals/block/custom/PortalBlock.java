package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;
import org.jetbrains.annotations.Nullable;

public class PortalBlock extends Block implements EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new PortalBlockEntity(pos, state)
                : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    public PortalBlockEntity getPortalData(Level level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        return (PortalBlockEntity) level.getBlockEntity(lowerPos);
    }



    // Breaking both portal blocks
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);

            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // Breaking portal when a block behind it gets destroyed
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);

        if (direction.getAxis() == Direction.Axis.Y) {
            boolean shouldBeOtherHalf = (direction == Direction.UP) == (half == DoubleBlockHalf.LOWER);
            if (shouldBeOtherHalf && (!neighborState.is(this) || neighborState.getValue(HALF) == half)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

}
