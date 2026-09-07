package net.nethredras.create_portals.item.custom.portal_gun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Tracks the bottom-block positions of the (at most 2) portals this
 * specific gun has open, oldest first. Attached to the ItemStack via
 * a DataComponent so it travels with the physical item.
 */
public record PortalGunData(List<BlockPos> portals) {

    public static final PortalGunData EMPTY = new PortalGunData(List.of());

    // Disk persistence (world save)
    public static final Codec<PortalGunData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.listOf().fieldOf("portals").forGetter(PortalGunData::portals)
    ).apply(instance, PortalGunData::new));

    // Network sync (server -> client, e.g. for HUD / tooltip display later)
    public static final StreamCodec<ByteBuf, PortalGunData> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), PortalGunData::portals,
            PortalGunData::new
    );
}
