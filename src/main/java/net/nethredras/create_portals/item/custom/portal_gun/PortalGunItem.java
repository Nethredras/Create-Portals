package net.nethredras.create_portals.item.custom.portal_gun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
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

import javax.annotation.Nullable;
import java.util.Optional;

public class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            firePortal(serverPlayer, stack, PortalColor.ORANGE);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public void firePortal(ServerPlayer player, ItemStack stack, PortalColor color) {
        ServerLevel level = (ServerLevel) player.level();

        double reach = 100;
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
     * unlinking whatever was previously there (which may be in a
     * different dimension), then re-links the pair if both colors are
     * now present.
     */
    private void handlePortalPlacement(ServerLevel level, ItemStack stack, BlockPos hitPos, Direction hitFace, PortalColor color) {
        MinecraftServer server = level.getServer();
        BlockPos newLowerPos = hitPos.relative(hitFace);
        GlobalPos newGlobalPos = GlobalPos.of(level.dimension(), newLowerPos);

        PortalGunData data = stack.getOrDefault(ModDataComponents.PORTAL_GUN_DATA.get(), PortalGunData.EMPTY);

        // Drop stale entries (portal was mined / broken without the gun being told)
        Optional<GlobalPos> bluePos = validate(server, data.bluePortal());
        Optional<GlobalPos> orangePos = validate(server, data.orangePortal());

        Optional<GlobalPos> targetSlot = color == PortalColor.BLUE ? bluePos : orangePos;
        Optional<GlobalPos> otherSlot = color == PortalColor.BLUE ? orangePos : bluePos;

        // Replace whatever was already in this color's slot
        if (targetSlot.isPresent()) {
            removePortal(server, targetSlot.get());
            otherSlot.ifPresent(pos -> unlinkPortal(server, pos));
        }

        placePortal(level, hitPos, hitFace, color);

        if (color == PortalColor.BLUE) {
            bluePos = Optional.of(newGlobalPos);
        } else {
            orangePos = Optional.of(newGlobalPos);
        }

        if (bluePos.isPresent() && orangePos.isPresent()) {
            linkPortals(server, bluePos.get(), orangePos.get());
        }

        stack.set(ModDataComponents.PORTAL_GUN_DATA.get(), new PortalGunData(bluePos, orangePos));
    }

    /**
     * Resolves a level from a server and dimension key. Returns null if
     * the dimension isn't currently loaded (shouldn't normally happen
     * for standard dimensions, but custom/removed dimensions could
     * theoretically go missing).
     */
    @Nullable
    private ServerLevel resolveLevel(MinecraftServer server, GlobalPos globalPos) {
        return server.getLevel(globalPos.dimension());
    }

    private Optional<GlobalPos> validate(MinecraftServer server, Optional<GlobalPos> globalPos) {
        return globalPos.filter(gp -> {
            ServerLevel targetLevel = resolveLevel(server, gp);
            return targetLevel != null && targetLevel.getBlockState(gp.pos()).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get());
        });
    }

    // Placing both portal halves — always in the level the player fired from
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

    // Removing both halves of a portal at its bottom-block global position
    private void removePortal(MinecraftServer server, GlobalPos globalPos) {
        ServerLevel targetLevel = resolveLevel(server, globalPos);
        if (targetLevel == null) {
            return;
        }
        BlockPos lowerPos = globalPos.pos();
        targetLevel.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        targetLevel.setBlock(lowerPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void linkPortals(MinecraftServer server, GlobalPos globalA, GlobalPos globalB) {
        ServerLevel levelA = resolveLevel(server, globalA);
        ServerLevel levelB = resolveLevel(server, globalB);

        if (levelA == null || levelB == null) {
            return;
        }

        BlockEntity beA = levelA.getBlockEntity(globalA.pos());
        BlockEntity beB = levelB.getBlockEntity(globalB.pos());

        if (beA instanceof PortalBlockEntity portalA && beB instanceof PortalBlockEntity portalB) {
            portalA.setLinkedPortal(globalB);
            portalB.setLinkedPortal(globalA);
        }
    }

    private void unlinkPortal(MinecraftServer server, GlobalPos globalPos) {
        ServerLevel targetLevel = resolveLevel(server, globalPos);
        if (targetLevel == null) {
            return;
        }
        if (targetLevel.getBlockEntity(globalPos.pos()) instanceof PortalBlockEntity portal) {
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