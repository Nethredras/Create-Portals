package net.nethredras.create_portals.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.item.custom.portal_gun.PortalGunItem;
import net.nethredras.create_portals.item.custom.portal_gun.PortalColor;
import net.nethredras.create_portals.network.FirePortalPacket;

@EventBusSubscriber(modid = CreatePortals.MOD_ID, value = Dist.CLIENT)
public class ClientPortalEvents {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return; // only care about left-click (attack)
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof PortalGunItem)) {
            return;
        }

        // Stop the normal attack/mining behavior from firing
        event.setCanceled(true);

        PacketDistributor.sendToServer(new FirePortalPacket(PortalColor.BLUE));
    }
}
