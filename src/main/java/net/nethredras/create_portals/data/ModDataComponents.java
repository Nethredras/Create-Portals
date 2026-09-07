package net.nethredras.create_portals.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.item.custom.portal_gun.PortalGunData;


import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreatePortals.MOD_ID);

    public static final Supplier<DataComponentType<PortalGunData>> PORTAL_GUN_DATA =
            DATA_COMPONENTS.register("portal_gun_data", () -> DataComponentType.<PortalGunData>builder()
                    .persistent(PortalGunData.CODEC)
                    .networkSynchronized(PortalGunData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
