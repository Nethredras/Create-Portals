package net.nethredras.create_portals.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import net.nethredras.create_portals.sound.ModSounds;
import net.nethredras.create_portals.util.PortalDetectionUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = CreatePortals.MOD_ID)
public class ModEvents {

    private static final PortalDetectionUtil DETECTOR = new PortalDetectionUtil();
    private static final Set<UUID> PLAYERS_IN_PORTAL = new HashSet<>();
    private static final double FLAT_PENETRATION_DEPTH = 0.95;
    private static final double SHOULDER_OFFSET_BELOW_EYES = 0.2;
    private static final double WALL_PENETRATION_DEPTH = 0.1;

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

    public static void playSound(Level level, Player player) {
        if ((int) (Math.random() * 2) + 1 == 1) {
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_ENTER1.get(), SoundSource.PLAYERS, 0.3F, 0.6F);
        } else {
            level.playSound(null, player.blockPosition(), ModSounds.PORTAL_ENTER2.get(), SoundSource.PLAYERS, 0.3F, 0.6F);
        }
    }

    /**
     * Returns the bottom (block-entity-owning) position of whichever
     * portal the player is currently positioned to trigger, or null.
     */
    private static BlockPos findTriggeredPortal(ServerLevel level, ServerPlayer player) {
        BlockPos wallTrigger = findWallTrigger(level, player);
        if (wallTrigger != null) {
            return wallTrigger;
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

    private static BlockPos findWallTrigger(ServerLevel level, ServerPlayer player) {
        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        Direction wallDir = DETECTOR.getPortalDirection(level, eyePos);
        if (wallDir == null || !isHorizontal(wallDir)) {
            return null;
        }

        Vec3 eyes = player.getEyePosition();
        double penetration = switch (wallDir) {
            case NORTH -> eyePos.getZ() + 1 - eyes.z; // eyes moving toward -Z
            case SOUTH -> eyes.z - eyePos.getZ();     // eyes moving toward +Z
            case EAST  -> eyes.x - eyePos.getX();     // eyes moving toward +X
            case WEST  -> eyePos.getX() + 1 - eyes.x; // eyes moving toward -X
            default -> 0;
        };

        if (penetration < WALL_PENETRATION_DEPTH) {
            return null; // not far enough into the wall block yet
        }

        return eyePos.below().relative(wallDir);
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
        ServerLevel sourceLevel = (ServerLevel) player.level();
        ServerLevel endPortalLevel = player.getServer().getLevel(linkedPos.dimension());
        if (endPortalLevel == null) {
            return;
        }

        BlockPos linkedPortalPos = linkedPos.pos();
        if (!(endPortalLevel.getBlockEntity(linkedPortalPos) instanceof PortalBlockEntity)) {
            return;
        }

        // Facing
        PortalOrientation sourceOrientation = getPortalOrientation(sourceLevel.getBlockState(sourcePortalPos));
        PortalOrientation destOrientation = getPortalOrientation(endPortalLevel.getBlockState(linkedPortalPos));
        float newYaw = player.getYRot() + computeYawDelta(sourceOrientation, destOrientation);

        // Velocity
        Vec3 endVelocity = getNewVelocity(player, sourceOrientation, destOrientation);

        // Teleport location
        Vec3 playerLocation = getPlayerPosAfterTeleport(destOrientation, linkedPortalPos, endPortalLevel);

        player.teleportTo(endPortalLevel, playerLocation.x, playerLocation.y, playerLocation.z,
                Set.of(), newYaw, player.getXRot());

        player.resetFallDistance();

        // Play sound
        playSound(endPortalLevel, player);


        player.setDeltaMovement(endVelocity);
        player.hurtMarked = true;
    }

    /**
     *
     * @param portalOrientation
     * @param portalPos
     * @param level
     * @return
     */
    public static Vec3 getPlayerPosAfterTeleport(PortalOrientation portalOrientation, BlockPos portalPos, Level level) {
        double positionX = portalPos.getX();
        double positionY = portalPos.getY();
        double positionZ = portalPos.getZ();
        Direction portalDirection = portalOrientation.reference;

        if (portalOrientation.isWall) {
            return getOffsetWall(positionX, positionY, positionZ, portalDirection);
        } else {
            portalDirection = level.getBlockState(portalPos).getValue(AbstractFlatPortalBlock.ORIENTATION);
            return getOffsetFloorCeiling(positionX, positionY, positionZ, portalDirection);
        }
    }

    /**
     * Returns the position where the player should be teleported to depending on the portal orientation
     * @param positionX
     * @param positionY
     * @param positionZ
     * @param portalDirection
     * @return
     */
    public static Vec3 getOffsetFloorCeiling(double positionX, double positionY, double positionZ, Direction portalDirection) {
        double offset = 0.5;
        double offset2 = 1;

        positionY -= 0.4;

        switch (portalDirection) {
            case NORTH:
                positionX += offset;
                break;
            case EAST:
                positionZ += offset;
                positionX += offset2;
                break;
            case SOUTH:
                positionX += offset;
                positionZ += offset2;
                break;
            case WEST:
                positionZ += offset;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + portalDirection);
        }
        return new Vec3(positionX, positionY, positionZ);
    }

    /**
     * Returns the position where the player should be teleported to depending on the portal orientation
     * @param positionX
     * @param positionY
     * @param positionZ
     * @param portalDirection
     * @return
     */
    public static Vec3 getOffsetWall(double positionX, double positionY, double positionZ, Direction portalDirection) {
        double offset = 0.5;
        double offset2 = 1;

        switch (portalDirection) {
            case NORTH:
                positionZ += offset2;
                positionX += offset;
                break;
            case EAST:
                positionZ += offset;
                break;
            case SOUTH:
                positionX += offset;
                break;
            case WEST:
                positionX += offset2;
                positionZ += offset;
                break;
        }

        return new Vec3(positionX, positionY, positionZ);
    }

    /**
     * Returns the player velocity after leaving the portal depending on the portal orientation
     * @param player
     * @param portalADirection
     * @param portalBDirection
     *
     * @return
     */
    public static Vec3 getNewVelocity(ServerPlayer player, PortalOrientation portalADirection, PortalOrientation portalBDirection) {
        double velocityX = 0;
        double velocityY = 0;
        double velocityZ = 0;

        double portalWallBoost = 0.2;
        double portalFloorBoost = 0.4;
        double portalCeilingBoost = 0.1;

        switch (portalBDirection.reference) {
            case NORTH:
                velocityZ = -portalWallBoost;
                break;
            case EAST:
                velocityX = portalWallBoost;
                break;
            case SOUTH:
                velocityZ = portalWallBoost;
                break;
            case WEST:
                velocityX = - portalWallBoost;
                break;
            case UP:
                velocityY = portalFloorBoost;
                break;
            case DOWN:
                velocityY = -portalCeilingBoost;
                break;
        }


        return new Vec3(velocityX, velocityY , velocityZ);
    }

    private static PortalOrientation getPortalOrientation(BlockState state) {
        if (state.getBlock() instanceof AbstractPortalBlock) {
            return new PortalOrientation(state.getValue(AbstractPortalBlock.FACING), true);
        }
        if (state.getBlock() instanceof AbstractFlatPortalBlock) {
            return new PortalOrientation(state.getValue(AbstractFlatPortalBlock.FACING), false);
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