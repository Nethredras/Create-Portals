package net.nethredras.create_portals.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.item.custom.portal_gun.PortalGunItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePortals.MOD_ID);

    // Portal Gun
    public static DeferredItem<Item> PORTAL_GUN = ITEMS.register("portal_gun",
            () -> new PortalGunItem(new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .stacksTo(1)));

    // Shards
    public static DeferredItem<Item> CRIMSON_SHARD = ITEMS.register("crimson_shard",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.RARE)));

    public static DeferredItem<Item> WARPED_SHARD = ITEMS.register("warped_shard",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.RARE)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
