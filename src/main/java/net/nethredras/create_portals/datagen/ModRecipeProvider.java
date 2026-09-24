package net.nethredras.create_portals.datagen;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.nethredras.create_portals.block.ModBlocks;
import net.nethredras.create_portals.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PORTAL_GUN.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', AllItems.BRASS_HAND.get())
                .unlockedBy("has_ingot", has(AllItems.BRASS_HAND))
                .save(recipeOutput);
    }
}
