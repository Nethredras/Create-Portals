package net.nethredras.create_portals.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.block.custom.PortalBlockBottom;
import net.nethredras.create_portals.block.custom.PortalBlockTop;
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
    public static final DeferredBlock<PortalBlockBottom> PORTAL_BLOCK_BOTTOM = BLOCKS.register("portal_block_bottom",
            () -> new PortalBlockBottom(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .noCollission()
                    .replaceable()));

    public static final DeferredBlock<PortalBlockTop> PORTAL_BLOCK_TOP = BLOCKS.register("portal_block_top",
            () -> new PortalBlockTop(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .noCollission()
                    .replaceable()));


    // Event Bus
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}