package net.nethredras.create_portals.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.block.custom.entity.PortalBlockEntity;
import net.nethredras.create_portals.mixin.PortalCollisionMixer;
import net.nethredras.create_portals.util.PortalDetectionUtil;

import java.util.*;

@EventBusSubscriber(modid = CreatePortals.MOD_ID)
public class ModEvents {
    private static Set<UUID> PLAYERS_IN_PORTAL = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        BlockPos eyeBlockPos = BlockPos.containing(serverPlayer.getEyePosition());

        boolean isPortalBlock = level.getBlockState(eyeBlockPos).is(ModBlocks.PORTAL_BLOCK_TOP.get())
                || level.getBlockState(eyeBlockPos).is(ModBlocks.PORTAL_BLOCK_BOTTOM.get());

        UUID playerId = serverPlayer.getUUID();

        // Teleports if the player is already inside the portal block
        PortalDetectionUtil portalDetectionUtil = new PortalDetectionUtil();
        Direction portalDirection = portalDetectionUtil.getPortalDirection(level, eyeBlockPos);

        if (portalDirection != null) {
            PLAYERS_IN_PORTAL.add(playerId);
            BlockPos portalPos = getPortalPos(eyeBlockPos.below(), portalDirection);

            if (!(level.getBlockEntity(portalPos) instanceof PortalBlockEntity portalBe) || !portalBe.isLinked()) {
                return;
            }

            GlobalPos linkedPos = portalBe.getLinkedPortal();

            teleportPlayer(serverPlayer, linkedPos);
            return;
        }




        // Check if the player's eye position is at the portal frame
        // TODO I'm maybe gonna add the other directions depending on how smooth the teleportation process is, for now they are disabled
        /*
        if (!isPortalBlock) {
            PLAYERS_IN_PORTAL.remove(playerId);
            return;
        }

        PLAYERS_IN_PORTAL.add(playerId);

        if (!(level.getBlockEntity(eyeBlockPos.below()) instanceof PortalBlockEntity portalBe) || !portalBe.isLinked()) {
            return;
        }


        Vec3 playerEyePosition = new Vec3(player.getEyePosition().get(Direction.Axis.X), player.getEyePosition().get(Direction.Axis.Y), player.getEyePosition().get(Direction.Axis.Z));

        // North
        BlockPos portalBlock = (level.getBlockEntity(eyeBlockPos.below())).getBlockPos();
        Vec3 portalBorder = new Vec3(portalBlock.getX(), portalBlock.getY(), portalBlock.getZ());

        if (Math.abs(playerEyePosition.get(Direction.Axis.Z)) > Math.abs(portalBorder.get(Direction.Axis.Z) + 0.95)) {
            return;
        }

        GlobalPos linkedPos = portalBe.getLinkedPortal();
        teleportPlayer(serverPlayer, linkedPos);
         */
    }

    public static void teleportPlayer(ServerPlayer player, GlobalPos linkedPos) {
        ServerLevel endPortalLevel = player.getServer().getLevel(linkedPos.dimension());

        if (endPortalLevel == null) {
            return;
        }

        BlockPos linkedPortalPos = linkedPos.pos();
        BlockEntity destinationBlockEntity = endPortalLevel.getBlockEntity(linkedPortalPos);

        if (!(destinationBlockEntity instanceof PortalBlockEntity destinationPortalBlockEntity)) {
            return;
        }



        player.teleportTo(linkedPortalPos.getX() + 0.5, linkedPortalPos.getY(), linkedPortalPos.getZ() + 0.5);
    }

    // Helper
    public static BlockPos getPortalPos(BlockPos initialPos, Direction portalDirection) {
        switch (portalDirection) {
            case NORTH:
                return initialPos.north(1);
            case WEST:
                return initialPos.west(1);
            case SOUTH:
                return initialPos.south(1);
            case EAST:
                return initialPos.east(1);
        }

        return null;
    }
}
