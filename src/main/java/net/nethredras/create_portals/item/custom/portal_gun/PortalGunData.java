package net.nethredras.create_portals.item.custom.portal_gun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Tracks the bottom-block positions of this gun's blue and orange
 * portals independently. Attached to the ItemStack via a DataComponent
 * so it travels with the physical item.
 */
public record PortalGunData(Optional<BlockPos> bluePortal, Optional<BlockPos> orangePortal) {

    public static final PortalGunData EMPTY = new PortalGunData(Optional.empty(), Optional.empty());

    // Disk persistence (world save)
    public static final Codec<PortalGunData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("blue_portal").forGetter(PortalGunData::bluePortal),
            BlockPos.CODEC.optionalFieldOf("orange_portal").forGetter(PortalGunData::orangePortal)
    ).apply(instance, PortalGunData::new));

    // Network sync (server -> client)
    public static final StreamCodec<ByteBuf, PortalGunData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), PortalGunData::bluePortal,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), PortalGunData::orangePortal,
            PortalGunData::new
    );

    public Optional<BlockPos> get(PortalColor color) {
        return color == PortalColor.BLUE ? bluePortal : orangePortal;
    }

    public PortalGunData with(PortalColor color, Optional<BlockPos> pos) {
        return color == PortalColor.BLUE
                ? new PortalGunData(pos, orangePortal)
                : new PortalGunData(bluePortal, pos);
    }
}