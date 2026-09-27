package net.nethredras.create_portals.worldgen;

import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModGeodePlacement {
    public static List<PlacementModifier> geodePlacement(int countOfPlacement, int heightLimitUpper, int heightLimitLower) {
        return List.of(
                RarityFilter.onAverageOnceEvery(20),
                CountPlacement.of(countOfPlacement),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(heightLimitLower), VerticalAnchor.absolute(heightLimitUpper)),
                BiomeFilter.biome()
        );
    }
}
