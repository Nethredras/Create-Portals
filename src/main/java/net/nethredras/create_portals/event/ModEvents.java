package net.nethredras.create_portals.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.block.custom.AbstractFlatPortalBlock;
import net.nethredras.create_portals.block.custom.AbstractPortalBlock;
import net.nethredras.create_portals.block.custom.FlatPortalBlockBottom;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;
import net.nethredras.create_portals.util.PortalDetectionUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = CreatePortals.MOD_ID)
public class ModEvents {

    private static final PortalDetectionUtil DETECTOR = new PortalDetectionUtil();
    private static final Set<UUID> PLAYERS_IN_PORTAL = new HashSet<>();

    // How far (0..1, exclusive of 1) the player sinks/rises into the solid
    // block behind a floor/ceiling portal before teleporting. Higher =
    // later trigger, more of the player model visibly submerged. Clamped
    // below 1.0 so there's always a margin before the hard safety line.
    private static final double FLAT_PENETRATION_DEPTH = Math.min(0.65, 0.95);

    // Rough distance from eye position down to shoulder height, used only
    // for timing the ceiling trigger — detection of "which portal cell is
    // this" still uses the full eye BlockPos, unaffected by this offset.
    private static final double SHOULDER_OFFSET_BELOW_EYES = 0.2;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        UUID playerId = serverPlayer.getUUID();

        BlockPos bottomPortalPos = findTriggeredPortal(level, serverPlayer);

        if (bottomPortalPos == null) {
            PLAYERS_IN_PORTAL.remove(playerId);
            return;
        }

        if (PLAYERS_IN_PORTAL.contains(playerId)) {
            return; // already fired for this pass-through
        }
        PLAYERS_IN_PORTAL.add(playerId);

        if (!(level.getBlockEntity(bottomPortalPos) instanceof PortalBlockEntity portalBe) || !portalBe.isLinked()) {
            return;
        }

        teleportPlayer(serverPlayer, bottomPortalPos, portalBe.getLinkedPortal());
    }

    /**
     * Returns the bottom (block-entity-owning) position of whichever
     * portal the player is currently positioned to trigger, or null.
     */
    private static BlockPos findTriggeredPortal(ServerLevel level, ServerPlayer player) {
        // Wall portals — unchanged, already works well.
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        Direction wallDir = DETECTOR.getPortalDirection(level, eyePos);
        if (wallDir != null && isHorizontal(wallDir)) {
            return eyePos.below().relative(wallDir);
        }

        BlockPos floorTrigger = findFlatTrigger(level, player, true);
        if (floorTrigger != null) {
            return floorTrigger;
        }
        return findFlatTrigger(level, player, false);
    }

    /**
     * isFloor = true handles a floor portal (player falls down through it,
     * surface block is below the portal cell). false handles a ceiling
     * portal (player rises up through it, surface block is above the
     * portal cell). Both phases of the fall/rise are covered: standing
     * right at the portal cell (not sunk yet) and already partway inside
     * the surface block.
     */
    private static BlockPos findFlatTrigger(ServerLevel level, ServerPlayer player, boolean isFloor) {
        BlockPos refBlockPos = isFloor
                ? BlockPos.containing(player.position())       // feet
                : BlockPos.containing(player.getEyePosition()); // eyes

        BlockPos portalCellPos;
        BlockPos surfaceBlockPos;

        if (level.getBlockState(refBlockPos).getBlock() instanceof AbstractFlatPortalBlock) {
            // Not sunk in yet — refBlockPos IS the portal cell.
            portalCellPos = refBlockPos;
            surfaceBlockPos = isFloor ? refBlockPos.below() : refBlockPos.above();
        } else {
            BlockPos neighborPos = isFloor ? refBlockPos.above() : refBlockPos.below();
            if (!(level.getBlockState(neighborPos).getBlock() instanceof AbstractFlatPortalBlock)) {
                return null; // no flat portal associated with this reference point at all
            }
            // Already sunk in — refBlockPos IS the surface block itself.
            portalCellPos = neighborPos;
            surfaceBlockPos = refBlockPos;
        }

        BlockState cellState = level.getBlockState(portalCellPos);
        BlockPos bottomPortalPos = resolveBottomFlatPortalPos(portalCellPos, cellState);

        int surfaceY = surfaceBlockPos.getY();

        if (isFloor) {
            double feetY = player.getY();
            double triggerY = surfaceY + 1 - FLAT_PENETRATION_DEPTH;
            boolean pastTunedLine = feetY <= triggerY;
            boolean pastSafetyLine = feetY <= surfaceY; // about to cross into the block beneath
            return (pastTunedLine || pastSafetyLine) ? bottomPortalPos : null;
        } else {
            double shoulderY = player.getEyePosition().y - SHOULDER_OFFSET_BELOW_EYES;
            double triggerY = surfaceY + FLAT_PENETRATION_DEPTH;
            boolean pastTunedLine = shoulderY >= triggerY;
            boolean pastSafetyLine = shoulderY >= surfaceY + 1; // about to cross into the block beyond
            return (pastTunedLine || pastSafetyLine) ? bottomPortalPos : null;
        }
    }

    private static BlockPos resolveBottomFlatPortalPos(BlockPos portalCellPos, BlockState cellState) {
        if (cellState.getBlock() instanceof FlatPortalBlockBottom) {
            return portalCellPos;
        }
        Direction orientation = cellState.getValue(AbstractFlatPortalBlock.ORIENTATION);
        return portalCellPos.relative(orientation.getOpposite());
    }

    private static boolean isHorizontal(Direction direction) {
        return direction != Direction.UP && direction != Direction.DOWN;
    }

    public static void teleportPlayer(ServerPlayer player, BlockPos sourcePortalPos, GlobalPos linkedPos) {
        ServerLevel endPortalLevel = player.getServer().getLevel(linkedPos.dimension());
        if (endPortalLevel == null) {
            return;
        }

        BlockPos linkedPortalPos = linkedPos.pos();
        BlockEntity destinationBlockEntity = endPortalLevel.getBlockEntity(linkedPortalPos);

        if (!(destinationBlockEntity instanceof PortalBlockEntity)) {
            return;
        }

        /**
         * Add velocity to player depending on the direction of the end portal
         */
        BlockState destinationBlockState = endPortalLevel.getBlockState(linkedPortalPos);
        Direction endPortalDest;

        // Base velocity
        Vec3 velocity = player.getDeltaMovement();
        double baseVelocityX = velocity.x();
        double baseVelocityY = velocity.y();
        double baseVelocityZ = velocity.z();


        // Extra velocity
        double extraVelocityX = 0.5;
        double extraVelocityY = 0.8;
        double extraVelocityZ = 0.5;

        // End velocity
        double endVelocityX = baseVelocityX;
        double endVelocityY = baseVelocityY;
        double endVelocityZ = baseVelocityZ;

        // Get end portal direction
        if (destinationBlockState.getBlock() instanceof AbstractFlatPortalBlock abstractFlatPortalBlock) {
            endPortalDest = destinationBlockState.getValue(AbstractFlatPortalBlock.FACING);
        } else {
            endPortalDest = destinationBlockState.getValue(AbstractPortalBlock.FACING);
        }

        // Calc player facing
        PortalOrientation sourceOrientation = getPortalOrientation(endPortalLevel.getBlockState(sourcePortalPos));
        PortalOrientation destOrientation = getPortalOrientation(endPortalLevel.getBlockState(linkedPortalPos));

        float newYaw = player.getYRot() + computeYawDelta(sourceOrientation, destOrientation);

        // Calc new velocity
        if (endPortalDest == Direction.UP || endPortalDest == Direction.DOWN) {
            endVelocityY = baseVelocityY + extraVelocityY;
        } else {
            endVelocityX = baseVelocityX + extraVelocityX;
        }



        // Teleport player
        player.teleportTo(endPortalLevel, linkedPortalPos.getX() + 0.5, linkedPortalPos.getY(), linkedPortalPos.getZ() + 0.5,
                Set.of(), newYaw, player.getXRot());

        // Apply velocity
        Vec3 endVelocity = new Vec3(endVelocityX, endVelocityY, endVelocityZ);

        player.setDeltaMovement(endVelocity);

        // Sync server and client
        player.hurtMarked = true;
    }

    private static PortalOrientation getPortalOrientation(BlockState state) {
        if (state.getBlock() instanceof AbstractPortalBlock) {
            return new PortalOrientation(state.getValue(AbstractPortalBlock.FACING), true);
        }
        if (state.getBlock() instanceof AbstractFlatPortalBlock) {
            return new PortalOrientation(state.getValue(AbstractFlatPortalBlock.ORIENTATION), false);
        }
        return null;
    }

    private static float computeYawDelta(PortalOrientation source, PortalOrientation dest) {
        if (source == null || dest == null || source.isWall() != dest.isWall()) {
            return 0f; // mixed wall<->flat transition: no rotation logic yet, keep facing as-is
        }

        float sourceYRot = source.reference().toYRot();
        float destYRot = dest.reference().toYRot();

        return source.isWall()
                ? destYRot - (sourceYRot + 180f)  // wall: player enters opposing FACING, exits along dest FACING
                : destYRot - sourceYRot;          // flat: no "opposing motion" flip needed
    }

    private record PortalOrientation(Direction reference, boolean isWall) {}
}