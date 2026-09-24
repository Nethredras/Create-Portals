package net.nethredras.create_portals.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.item.ModItems;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreatePortals.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
    }
}
