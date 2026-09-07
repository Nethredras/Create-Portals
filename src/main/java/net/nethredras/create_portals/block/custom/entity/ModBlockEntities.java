package net.nethredras.create_portals.block.custom.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreatePortals.MOD_ID);

    public static final Supplier<BlockEntityType<PortalBlockEntity>> PORTAL_BE =
            BLOCK_ENTITIES.register("portal_be", () -> BlockEntityType.Builder.of(
                    PortalBlockEntity::new, ModBlocks.PORTAL_BLOCK_BOTTOM.get()).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
