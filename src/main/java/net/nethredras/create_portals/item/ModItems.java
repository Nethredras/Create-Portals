package net.nethredras.create_portals.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePortals.MOD_ID);



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
