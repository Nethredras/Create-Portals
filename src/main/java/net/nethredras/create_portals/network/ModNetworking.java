package net.nethredras.create_portals.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.nethredras.create_portals.CreatePortals;

@EventBusSubscriber(modid = CreatePortals.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CreatePortals.MOD_ID).versioned("1.0");

        registrar.playToServer(
                FirePortalPacket.TYPE,
                FirePortalPacket.STREAM_CODEC,
                FirePortalPacket::handle
        );
    }
}
