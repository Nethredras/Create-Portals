package net.nethredras.create_portals.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.block.custom.PortalBlock;
import net.nethredras.create_portals.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreatePortals.MOD_ID);

    // Helper methods for also adding an item to the block
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    // Register Blocks
    public static DeferredBlock<Block> PORTAL_BLOCK = BLOCKS.register("portal_block",
            () -> new PortalBlock(BlockBehaviour.Properties.of().noOcclusion().noCollission()));



    // Event Bus
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
