package net.nethredras.create_portals.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.item.custom.PortalGunItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePortals.MOD_ID);

    public static DeferredItem<Item> PORTAL_GUN = ITEMS.register("portal_gun",
            () -> new PortalGunItem(new Item.Properties().rarity(Rarity.EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
