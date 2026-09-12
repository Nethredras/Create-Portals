package net.nethredras.create_portals.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.AbstractPortalBlock;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;
import net.nethredras.create_portals.util.PortalDetectionUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PortalCollisionMixer {

    // Injections
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    public void removeCollisionShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        PortalDetectionUtil portalDetectionUtil = new PortalDetectionUtil();
        Direction portalDirection = portalDetectionUtil.getPortalDirection(level, pos);

        if (portalDirection != null) {
            VoxelShape shape;

            if (portalIsLow(level, pos, portalDirection)) {
                if (!isLinked(level, pos, portalDirection)) {
                    shape = Block.box(0, 0, 0, 16, 16, 16);
                } else {
                    shape = makeLowerShape(portalDirection);

                }
            } else {
                shape = makeUpperShape(portalDirection);
            }

            cir.setReturnValue(shape);
            cir.cancel();
        }
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void removeCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        PortalDetectionUtil portalDetectionUtil = new PortalDetectionUtil();
        Direction portalDirection = portalDetectionUtil.getPortalDirection(level, pos);

        if (portalDirection != null) {
            VoxelShape shape;

            if (portalIsLow(level, pos, portalDirection)) {
                if (!isLinked(level, pos, portalDirection)) {
                    shape = Block.box(0, 0, 0, 16, 16, 16);
                } else {
                    shape = makeLowerShape(portalDirection);
                }
            } else {
                shape = makeUpperShape(portalDirection);
            }

            cir.setReturnValue(shape);
            cir.cancel();
        }
    }

    // Remove portalBlock suffocation
    @Inject(method = "isSuffocating(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void removeSuffocation(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PortalDetectionUtil portalDetectionUtil = new PortalDetectionUtil();
        if (portalDetectionUtil.getPortalDirection(level, pos) != null) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }


    // Helper Methods
    public boolean isLinked(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos northPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
        BlockPos eastPos = new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
        BlockPos southPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
        BlockPos westPos = new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());

        if ((level.getBlockEntity(northPos) instanceof PortalBlockEntity N) && N.isLinked()) {
            return true;
        } else if ((level.getBlockEntity(eastPos) instanceof PortalBlockEntity E) && E.isLinked()) {
            return true;
        } else if ((level.getBlockEntity(southPos) instanceof PortalBlockEntity S) && S.isLinked()) {
            return true;
        } else if ((level.getBlockEntity(westPos) instanceof PortalBlockEntity W) && W.isLinked()) {
            return true;
        }

        return false;
    }

    public boolean portalIsLow(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos northPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
        BlockPos eastPos = new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
        BlockPos southPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
        BlockPos westPos = new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());

        if (level.getBlockState(northPos).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get())) {
            return true;
        } else if (level.getBlockState(eastPos).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get())) {
            return true;
        } else if (level.getBlockState(southPos).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get())) {
            return true;
        } else if (level.getBlockState(westPos).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get())) {
            return true;
        }

        return false;
    }


    // Shapes
    public VoxelShape makeLowerShape(Direction direction){
        VoxelShape shape = Shapes.empty();

        switch (direction) {
            case NORTH:
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.0625, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.9375, 0, 0, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0.9375, 0.9375, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0, 0.9375, 0.0625, 0.9375), BooleanOp.OR);
                break;
            case EAST:
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.0625, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 1, 1, 0.0625), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0.9375, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0, 0.9375, 0.0625, 0.9375), BooleanOp.OR);
                break;
            case SOUTH:
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.0625, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.9375, 0, 0, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0, 0.9375, 1, 0.0625), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0, 0.9375, 0.0625, 0.9375), BooleanOp.OR);
                break;
            case WEST:
                shape = Shapes.join(shape, Shapes.box(0.9375, 0, 0, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 1, 1, 0.0625), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0.9375, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0, 0.9375, 0.0625, 0.9375), BooleanOp.OR);
                break;
        }

        return shape;
    }

    public VoxelShape makeUpperShape(Direction direction){
        VoxelShape shape = Shapes.empty();

        switch (direction) {
            case NORTH:
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.0625, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.9375, 0, 0, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0.9375, 0.9375, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0.9375, 0, 1, 1, 1), BooleanOp.OR);
                break;
            case EAST:
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.0625, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 1, 1, 0.0625), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0, 0.9375, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0.9375, 0, 1, 1, 1), BooleanOp.OR);
                break;
            case SOUTH:
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 0.0625, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.9375, 0, 0, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0, 0.9375, 1, 0.0625), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0.9375, 0, 1, 1, 1), BooleanOp.OR);
                break;
            case WEST:
                shape = Shapes.join(shape, Shapes.box(0.9375, 0, 0, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0, 0, 1, 1, 0.0625), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0, 0.9375, 1, 1, 1), BooleanOp.OR);
                shape = Shapes.join(shape, Shapes.box(0, 0.9375, 0, 1, 1, 1), BooleanOp.OR);
                break;
        }

        return shape;
    }
}
