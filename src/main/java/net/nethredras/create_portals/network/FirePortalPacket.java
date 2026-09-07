package net.nethredras.create_portals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.item.custom.portal_gun.PortalGunItem;
import net.nethredras.create_portals.item.custom.portal_gun.PortalColor;

public record FirePortalPacket(PortalColor color) implements CustomPacketPayload {

    public static final Type<FirePortalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePortals.MOD_ID, "fire_portal"));

    public static final StreamCodec<ByteBuf, FirePortalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> PortalColor.values()[id], PortalColor::ordinal),
            FirePortalPacket::color,
            FirePortalPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Runs on the server after the packet arrives.
    public static void handle(FirePortalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ItemStack stack = serverPlayer.getMainHandItem();
                if (stack.getItem() instanceof PortalGunItem gun) {
                    gun.firePortal(serverPlayer, stack, packet.color());
                }
            }
        });
    }
}
