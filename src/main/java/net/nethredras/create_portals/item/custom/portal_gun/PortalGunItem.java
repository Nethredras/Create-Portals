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
import net.nethredras.create_portals.block.custom.AbstractFlatPortalBlock;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;
import net.nethredras.create_portals.data.ModDataComponents;
import net.nethredras.create_portals.sound.ModSounds;

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

        if (color == PortalColor.BLUE) {
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_FIRED1.get(), SoundSource.PLAYERS, 0.3F, 0.6F);
        } else {
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_FIRED2.get(), SoundSource.PLAYERS, 0.3F, 0.6F);
        }

        BlockPos hitPos = hit.getBlockPos();
        Direction hitFace = hit.getDirection();

        boolean isFlat = hitFace == Direction.UP || hitFace == Direction.DOWN;

        if (isFlat) {
            // Nearest cardinal from the player's own facing — decides
            // which horizontal direction the second half extends toward.
            Direction orientation = player.getDirection();

            if (canPlaceFlatPortal(level, hitPos, hitFace, orientation)) {
                handleFlatPortalPlacement(level, stack, hitPos, hitFace, orientation, color);
            } else {
                denySound(level, player);
            }
        } else {
            if (canPlacePortal(level, hitPos, hitFace)) {
                handlePortalPlacement(level, stack, hitPos, hitFace, color);
            } else {
                denySound(level, player);
            }
        }
    }

    private void denySound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), ModSounds.PORTAL_DENIED.get(), SoundSource.PLAYERS, 0.5F, 0.8F);
    }

    // --- Wall portal placement ---

    private void handlePortalPlacement(ServerLevel level, ItemStack stack, BlockPos hitPos, Direction hitFace, PortalColor color) {
        handlePlacement(level, stack, hitPos, hitFace, color,
                () -> placePortal(level, hitPos, hitFace, color));
    }

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

    // --- Flat (floor/ceiling) portal placement ---

    private void handleFlatPortalPlacement(ServerLevel level, ItemStack stack, BlockPos hitPos, Direction hitFace,
                                           Direction orientation, PortalColor color) {
        handlePlacement(level, stack, hitPos, hitFace, color,
                () -> placeFlatPortal(level, hitPos, hitFace, orientation, color));
    }

    private void placeFlatPortal(ServerLevel level, BlockPos hitPos, Direction hitFace, Direction orientation, PortalColor color) {
        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos secondPos = lowerPos.relative(orientation);

        BlockState lowerState = ModBlocks.FLAT_PORTAL_BLOCK_BOTTOM.get().defaultBlockState()
                .setValue(AbstractFlatPortalBlock.FACING, hitFace)
                .setValue(AbstractFlatPortalBlock.ORIENTATION, orientation)
                .setValue(AbstractFlatPortalBlock.COLOR, color);

        BlockState secondState = ModBlocks.FLAT_PORTAL_BLOCK_TOP.get().defaultBlockState()
                .setValue(AbstractFlatPortalBlock.FACING, hitFace)
                .setValue(AbstractFlatPortalBlock.ORIENTATION, orientation)
                .setValue(AbstractFlatPortalBlock.COLOR, color);

        level.setBlock(lowerPos, lowerState, Block.UPDATE_ALL);
        level.setBlock(secondPos, secondState, Block.UPDATE_ALL);
    }

    // --- Shared placement flow (evict old slot, place, relink, save gun data) ---

    private void handlePlacement(ServerLevel level, ItemStack stack, BlockPos hitPos, Direction hitFace,
                                 PortalColor color, Runnable placer) {
        MinecraftServer server = level.getServer();
        BlockPos newLowerPos = hitPos.relative(hitFace);
        GlobalPos newGlobalPos = GlobalPos.of(level.dimension(), newLowerPos);

        PortalGunData data = stack.getOrDefault(ModDataComponents.PORTAL_GUN_DATA.get(), PortalGunData.EMPTY);

        Optional<GlobalPos> bluePos = validate(server, data.bluePortal());
        Optional<GlobalPos> orangePos = validate(server, data.orangePortal());

        Optional<GlobalPos> targetSlot = color == PortalColor.BLUE ? bluePos : orangePos;
        Optional<GlobalPos> otherSlot = color == PortalColor.BLUE ? orangePos : bluePos;

        if (targetSlot.isPresent()) {
            removePortal(server, targetSlot.get());
            otherSlot.ifPresent(pos -> unlinkPortal(server, pos));
        }

        placer.run();

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

    @Nullable
    private ServerLevel resolveLevel(MinecraftServer server, GlobalPos globalPos) {
        return server.getLevel(globalPos.dimension());
    }

    private Optional<GlobalPos> validate(MinecraftServer server, Optional<GlobalPos> globalPos) {
        return globalPos.filter(gp -> {
            ServerLevel targetLevel = resolveLevel(server, gp);
            if (targetLevel == null) {
                return false;
            }
            BlockState state = targetLevel.getBlockState(gp.pos());
            return state.is(ModBlocks.PORTAL_BLOCK_BOTTOM.get()) || state.is(ModBlocks.FLAT_PORTAL_BLOCK_BOTTOM.get());
        });
    }

    /**
     * Removes both halves of whatever portal (wall or flat) sits at this
     * global position, figuring out the second half's location from the
     * bottom block's own type and stored orientation rather than
     * assuming a fixed relationship.
     */
    private void removePortal(MinecraftServer server, GlobalPos globalPos) {
        ServerLevel targetLevel = resolveLevel(server, globalPos);
        if (targetLevel == null) {
            return;
        }

        BlockPos lowerPos = globalPos.pos();
        BlockState lowerState = targetLevel.getBlockState(lowerPos);

        BlockPos secondPos;
        if (lowerState.is(ModBlocks.PORTAL_BLOCK_BOTTOM.get())) {
            secondPos = lowerPos.above();
        } else if (lowerState.is(ModBlocks.FLAT_PORTAL_BLOCK_BOTTOM.get())) {
            Direction orientation = lowerState.getValue(AbstractFlatPortalBlock.ORIENTATION);
            secondPos = lowerPos.relative(orientation);
        } else {
            // Already gone or not a recognized bottom block — nothing to do
            return;
        }

        targetLevel.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        targetLevel.setBlock(secondPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
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

    // --- Shared validity checks (used by both wall and flat placement) ---

    private boolean isValidWallFace(Level level, BlockPos wallPos, Direction face) {
        BlockState state = level.getBlockState(wallPos);

        if (state.isAir()) {
            return false;
        }

        BlockState directionalState;

        switch (face) {
            case NORTH:
                directionalState = level.getBlockState(wallPos.north());
                break;
            case EAST:
                directionalState = level.getBlockState(wallPos.east());
                break;
            case SOUTH:
                directionalState = level.getBlockState(wallPos.south());
                break;
            case WEST:
                directionalState = level.getBlockState(wallPos.west());
                break;
            case DOWN:
                directionalState = level.getBlockState(wallPos.below());
                break;
            default:
                directionalState = level.getBlockState(wallPos.above());
                break;
        }

        if (directionalState.getBlock() instanceof AbstractPortalBlock || directionalState.getBlock() instanceof AbstractFlatPortalBlock) {
            return true;
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

        return state.canBeReplaced();
    }

    public boolean canPlacePortal(Level level, BlockPos hitPos, Direction hitFace) {
        if (!isValidWallSurface(level, hitPos, hitFace)) {
            return false;
        }

        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos upperPos = lowerPos.above();

        return isSpaceValid(level, lowerPos) && isSpaceValid(level, upperPos);
    }

    public boolean canPlaceFlatPortal(Level level, BlockPos hitPos, Direction hitFace, Direction orientation) {
        BlockPos secondFloorPos = hitPos.relative(orientation);

        if (!isValidWallFace(level, hitPos, hitFace) || !isValidWallFace(level, secondFloorPos, hitFace)) {
            return false;
        }

        BlockPos lowerPos = hitPos.relative(hitFace);
        BlockPos secondPos = lowerPos.relative(orientation);

        return isSpaceValid(level, lowerPos) && isSpaceValid(level, secondPos);
    }
}