package net.nethredras.create_portals.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nethredras.create_portals.item.custom.portal_gun.PortalColor;

public abstract class AbstractFlatPortalBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.UP, Direction.DOWN);

    public static final DirectionProperty ORIENTATION = DirectionProperty.create("orientation", Direction.Plane.HORIZONTAL);

    public static final EnumProperty<PortalColor> COLOR = EnumProperty.create("color", PortalColor.class);
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 0, 0, 0);

    public AbstractFlatPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.UP)
                .setValue(ORIENTATION, Direction.NORTH)
                .setValue(COLOR, PortalColor.BLUE));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ORIENTATION, COLOR);
    }

    protected boolean surfaceBehindStillValid(BlockState state, Direction direction, BlockState neighborState,
                                              LevelAccessor level, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);

        if (direction == facing.getOpposite()) {
            return !neighborState.isAir()
                    && Block.isFaceFull(neighborState.getCollisionShape(level, neighborPos), facing);
        }

        return true;
    }
}
