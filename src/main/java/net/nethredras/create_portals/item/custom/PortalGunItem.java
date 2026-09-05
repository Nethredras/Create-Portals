package net.nethredras.create_portals.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.PortalBlock;

public class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            double reach = 100; // Reach of gun
            Vec3 eyePos = player.getEyePosition();
            Vec3 viewVec = player.getViewVector(1.0f);
            Vec3 endPos = eyePos.add(viewVec.scale(reach));

            ClipContext ctx = new ClipContext(
                    eyePos,
                    endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            );

            BlockHitResult hit = level.clip(ctx);

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                Direction hitFace = hit.getDirection();

                if (canPlacePortal(level, hitPos, hitFace)) {
                    Direction facing = hitFace; // portal visually faces back toward player
                    placePortal((ServerLevel) level, hitPos, hitFace, facing);
                } else {
                    // deny feedback — sound, particle, or message
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // Placing both portal blocks
    public void placePortal(ServerLevel level, BlockPos hitPos, Direction hitFace, Direction facing) {
        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos upperPos = lowerPos.above();

        BlockState lowerState = ModBlocks.PORTAL_BLOCK.get().defaultBlockState()
                .setValue(PortalBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(PortalBlock.FACING, facing);

        BlockState upperState = lowerState.setValue(PortalBlock.HALF, DoubleBlockHalf.UPPER);

        level.setBlock(lowerPos, lowerState, Block.UPDATE_ALL);
        level.setBlock(upperPos, upperState, Block.UPDATE_ALL);
    }

    private boolean isValidWallFace(Level level, BlockPos wallPos, Direction face) {
        BlockState state = level.getBlockState(wallPos);

        if (state.isAir()) {
            return false;
        }

        return Block.isFaceFull(state.getCollisionShape(level, wallPos), face);
    }

    private boolean isValidWallSurface(Level level, BlockPos hitPos, Direction face) {
        BlockPos lowerWall = hitPos;
        BlockPos upperWall = hitPos.above();

        return isValidWallFace(level, lowerWall, face)
                && isValidWallFace(level, upperWall, face);
    }

    private boolean isSpaceValid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Must be replaceable (air, tall grass, water counts if you allow that, etc.)
        if (!state.canBeReplaced()) {
            return false;
        }

        // Must not already be a portal (either half)
        if (state.is(ModBlocks.PORTAL_BLOCK.get())) {
            return false;
        }

        return true;
    }

    public boolean canPlacePortal(Level level, BlockPos hitPos, Direction hitFace) {
        // 1. Wall behind must be solid & flat, both halves
        if (!isValidWallSurface(level, hitPos, hitFace)) {
            return false;
        }

        // 2. Compute where the portal itself will sit
        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos upperPos = lowerPos.above();

        // 3. Both target spaces must be free
        if (!isSpaceValid(level, lowerPos) || !isSpaceValid(level, upperPos)) {
            return false;
        }

        return true;
    }

}


