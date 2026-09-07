package net.nethredras.create_portals.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.AbstractPortalBlock;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;
import net.nethredras.create_portals.data.ModDataComponents;
import net.nethredras.create_portals.item.custom.portal_gun.PortalGunData;

import java.util.ArrayList;
import java.util.List;

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

                // Reject floor/ceiling hits — walls only for now
                if (hitFace == Direction.UP || hitFace == Direction.DOWN) {
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }

                if (canPlacePortal(level, hitPos, hitFace)) {
                    handlePortalPlacement((ServerLevel) level, stack, hitPos, hitFace);
                } else {
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Top-level flow for firing the gun: clean up stale references,
     * evict the oldest portal if we're already at the 2-portal cap,
     * place the new one, and link the remaining pair (if any).
     */
    private void handlePortalPlacement(ServerLevel level, ItemStack stack, BlockPos hitPos, Direction hitFace) {
        BlockPos newLowerPos = hitPos.relative(hitFace);

        PortalGunData data = stack.getOrDefault(ModDataComponents.PORTAL_GUN_DATA.get(), PortalGunData.EMPTY);
        List<BlockPos> portals = new ArrayList<>(data.portals());

        // Drop any entries that no longer point at an actual portal block
        // (e.g. it was mined, or its wall/other half broke via updateShape)
        portals.removeIf(pos -> !level.getBlockState(pos).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get()));

        // At the cap: evict the oldest portal to make room
        if (portals.size() >= 2) {
            BlockPos oldest = portals.remove(0);
            removePortal(level, oldest);

            // Whatever portal remains was linked to the one we just
            // removed — that link is now dangling, so clear it.
            if (!portals.isEmpty()) {
                unlinkPortal(level, portals.get(0));
            }
        }

        placePortal(level, hitPos, hitFace);
        portals.add(newLowerPos);

        // If we now have exactly 2 open portals, link them to each other
        if (portals.size() == 2) {
            linkPortals(level, portals.get(0), portals.get(1));
        }

        stack.set(ModDataComponents.PORTAL_GUN_DATA.get(), new PortalGunData(List.copyOf(portals)));
    }

    // Placing both portal halves
    private void placePortal(ServerLevel level, BlockPos hitPos, Direction hitFace) {
        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos upperPos = lowerPos.above();

        BlockState lowerState = ModBlocks.PORTAL_BLOCK_BOTTOM.get().defaultBlockState()
                .setValue(AbstractPortalBlock.FACING, hitFace);

        BlockState upperState = ModBlocks.PORTAL_BLOCK_TOP.get().defaultBlockState()
                .setValue(AbstractPortalBlock.FACING, hitFace);

        level.setBlock(lowerPos, lowerState, Block.UPDATE_ALL);
        level.setBlock(upperPos, upperState, Block.UPDATE_ALL);
    }

    // Removing both halves of a portal at its bottom-block position
    private void removePortal(Level level, BlockPos lowerPos) {
        level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(lowerPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void linkPortals(Level level, BlockPos posA, BlockPos posB) {
        BlockEntity beA = level.getBlockEntity(posA);
        BlockEntity beB = level.getBlockEntity(posB);

        if (beA instanceof PortalBlockEntity portalA && beB instanceof PortalBlockEntity portalB) {
            portalA.setLinkedPortal(posB);
            portalB.setLinkedPortal(posA);
        }
    }

    private void unlinkPortal(Level level, BlockPos lowerPos) {
        if (level.getBlockEntity(lowerPos) instanceof PortalBlockEntity portal) {
            portal.unlink();
        }
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

        if (!state.canBeReplaced()) {
            return false;
        }

        if (state.is(ModBlocks.PORTAL_BLOCK_BOTTOM.get()) || state.is(ModBlocks.PORTAL_BLOCK_TOP.get())) {
            return false;
        }

        return true;
    }

    public boolean canPlacePortal(Level level, BlockPos hitPos, Direction hitFace) {
        if (!isValidWallSurface(level, hitPos, hitFace)) {
            return false;
        }

        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos upperPos = lowerPos.above();

        if (!isSpaceValid(level, lowerPos) || !isSpaceValid(level, upperPos)) {
            return false;
        }

        return true;
    }
}