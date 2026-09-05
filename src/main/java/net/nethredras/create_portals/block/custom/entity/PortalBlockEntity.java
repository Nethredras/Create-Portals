package net.nethredras.create_portals.block.custom.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PortalBlockEntity extends BlockEntity {
    private BlockPos linkedPortal;
    private Direction facing;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_BE.get(), pos, state);
    }

}
