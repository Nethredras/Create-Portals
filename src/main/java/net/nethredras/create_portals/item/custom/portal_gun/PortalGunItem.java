package net.nethredras.create_portals.item.custom.portal_gun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.Optional;

public class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    // Right-click always fires the orange portal — vanilla's use() hook
    // already only ever runs server-side-authoritatively, so no packet
    // is needed for this one.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            firePortal(serverPlayer, stack, PortalColor.ORANGE);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Entry point for firing either colored portal, called either from
     * use() (orange, right-click) or from FirePortalPacket's server-side
     * handler (blue, left-click — reported by ClientPortalEvents since
     * vanilla has no built-in left-click item hook).
     */
    public void firePortal(ServerPlayer player, ItemStack stack, PortalColor color) {
        ServerLevel level = (ServerLevel) player.level();

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

        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos hitPos = hit.getBlockPos();
        Direction hitFace = hit.getDirection();

        // Reject floor/ceiling hits — walls only for now
        if (hitFace == Direction.UP || hitFace == Direction.DOWN) {
            denySound(level, player);
            return;
        }

        if (canPlacePortal(level, hitPos, hitFace)) {
            handlePortalPlacement(level, stack, hitPos, hitFace, color);
        } else {
            denySound(level, player);
        }
    }

    private void denySound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
    }

    /**
     * Places the new portal in the given color's slot, evicting and
     * unlinking whatever was previously there, then re-links the pair
     * if both colors are now present.
     */
    private void handlePortalPlacement(ServerLevel level, ItemStack stack, BlockPos hitPos, Direction hitFace, PortalColor color) {
        BlockPos newLowerPos = hitPos.relative(hitFace);

        PortalGunData data = stack.getOrDefault(ModDataComponents.PORTAL_GUN_DATA.get(), PortalGunData.EMPTY);

        // Drop stale entries (portal was mined / broken by updateShape
        // without the gun being told)
        Optional<BlockPos> bluePos = validate(level, data.bluePortal());
        Optional<BlockPos> orangePos = validate(level, data.orangePortal());

        Optional<BlockPos> targetSlot = color == PortalColor.BLUE ? bluePos : orangePos;
        Optional<BlockPos> otherSlot = color == PortalColor.BLUE ? orangePos : bluePos;

        // Replace whatever was already in this color's slot
        if (targetSlot.isPresent()) {
            removePortal(level, targetSlot.get());
            otherSlot.ifPresent(pos -> unlinkPortal(level, pos));
        }

        placePortal(level, hitPos, hitFace, color);

        if (color == PortalColor.BLUE) {
            bluePos = Optional.of(newLowerPos);
        } else {
            orangePos = Optional.of(newLowerPos);
        }

        if (bluePos.isPresent() && orangePos.isPresent()) {
            linkPortals(level, bluePos.get(), orangePos.get());
        }

        stack.set(ModDataComponents.PORTAL_GUN_DATA.get(), new PortalGunData(bluePos, orangePos));
    }

    private Optional<BlockPos> validate(ServerLevel level, Optional<BlockPos> pos) {
        return pos.filter(p -> level.getBlockState(p).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get()));
    }

    // Placing both portal halves
    private void placePortal(ServerLevel level, BlockPos hitPos, Direction hitFace, PortalColor color) {
        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos upperPos = lowerPos.above();

        BlockState lowerState = ModBlocks.PORTAL_BLOCK_BOTTOM.get().defaultBlockState()
                .setValue(AbstractPortalBlock.FACING, hitFace)
                .setValue(AbstractPortalBlock.COLOR, color);

        BlockState upperState = ModBlocks.PORTAL_BLOCK_TOP.get().defaultBlockState()
                .setValue(AbstractPortalBlock.FACING, hitFace)
                .setValue(AbstractPortalBlock.COLOR, color);

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