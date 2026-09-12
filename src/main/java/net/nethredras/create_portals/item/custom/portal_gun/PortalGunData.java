package net.nethredras.create_portals.item.custom.portal_gun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Tracks the bottom-block global positions (dimension + pos) of this
 * gun's blue and orange portals independently. Attached to the ItemStack
 * via a DataComponent so it travels with the physical item.
 */
public record PortalGunData(Optional<GlobalPos> bluePortal, Optional<GlobalPos> orangePortal) {

    public static final PortalGunData EMPTY = new PortalGunData(Optional.empty(), Optional.empty());

    // Disk persistence (world save)
    public static final Codec<PortalGunData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("blue_portal").forGetter(PortalGunData::bluePortal),
            GlobalPos.CODEC.optionalFieldOf("orange_portal").forGetter(PortalGunData::orangePortal)
    ).apply(instance, PortalGunData::new));

    // Network sync (server -> client)
    public static final StreamCodec<ByteBuf, PortalGunData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), PortalGunData::bluePortal,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), PortalGunData::orangePortal,
            PortalGunData::new
    );

    public Optional<GlobalPos> get(PortalColor color) {
        return color == PortalColor.BLUE ? bluePortal : orangePortal;
    }

    public PortalGunData with(PortalColor color, Optional<GlobalPos> pos) {
        return color == PortalColor.BLUE
                ? new PortalGunData(pos, orangePortal)
                : new PortalGunData(bluePortal, pos);
    }
}