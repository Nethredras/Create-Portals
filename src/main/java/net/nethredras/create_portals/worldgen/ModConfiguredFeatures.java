package net.nethredras.create_portals.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.nethredras.create_portals.CreatePortals;
import net.nethredras.create_portals.block.ModBlocks;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPED_GEODE_KEY = registerKey("warped_geode_placed");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRIMSON_GEODE_KEY = registerKey("crimson_geode_placed");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // Warped Geode
        register(context, WARPED_GEODE_KEY, Feature.GEODE, new GeodeConfiguration(new GeodeBlockSettings(
                BlockStateProvider.simple(Blocks.AIR),
                BlockStateProvider.simple(ModBlocks.WARPED_AMETHYST_BLOCK.get()),
                BlockStateProvider.simple(Blocks.SHROOMLIGHT),
                BlockStateProvider.simple(Blocks.CALCITE),
                BlockStateProvider.simple(Blocks.SMOOTH_BASALT),
                List.of(Blocks.SHROOMLIGHT.defaultBlockState()),
                BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS),
                new GeodeLayerSettings(6, 8.7, 7.5, 15),
                new GeodeCrackSettings(0.5, 2.0, 2), 0.35, 0.083,
                true,
                UniformInt.of(4, 6),
                UniformInt.of(3, 4),
                UniformInt.of(1, 2),
                -16, 16, 0.02, 1));

        // Crimson Geode
        register(context, CRIMSON_GEODE_KEY, Feature.GEODE, new GeodeConfiguration(new GeodeBlockSettings(
                BlockStateProvider.simple(Blocks.AIR),
                BlockStateProvider.simple(ModBlocks.CRIMSON_AMETHYST_BLOCK.get()),
                BlockStateProvider.simple(Blocks.SHROOMLIGHT),
                BlockStateProvider.simple(Blocks.CALCITE),
                BlockStateProvider.simple(Blocks.SMOOTH_BASALT),
                List.of(Blocks.SHROOMLIGHT.defaultBlockState()),
                BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS),
                new GeodeLayerSettings(6, 8.7, 7.5, 15),
                new GeodeCrackSettings(0.5, 2.0, 2), 0.35, 0.083,
                true,
                UniformInt.of(4, 6),
                UniformInt.of(3, 4),
                UniformInt.of(1, 2),
                -16, 16, 0.02, 1));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(CreatePortals.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
