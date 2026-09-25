package net.nethredras.create_portals.worldgen;

import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.List;

public class ModGeodePlacement {
    public static List<PlacementModifier> geodePlacement(int countOfPlacement, int heightLimitUpper, int heightLimitLower) {
        return List.of(CountPlacement.of(countOfPlacement), HeightRangePlacement.uniform(VerticalAnchor.absolute(heightLimitLower), VerticalAnchor.absolute(heightLimitUpper)), BiomeFilter.biome());
    }
}
