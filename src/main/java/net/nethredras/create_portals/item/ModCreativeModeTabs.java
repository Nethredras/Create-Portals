package net.nethredras.create_portals.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreatePortals.MOD_ID);

    public static final Supplier<CreativeModeTab> CREATE_PORTALS_TAB = CREATIVE_MODE_TAB.register("create_portals_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(Items.COMPASS))
                    .title(Component.translatable("Create: Portals"))
                    .displayItems((parameters, output) -> {




                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
