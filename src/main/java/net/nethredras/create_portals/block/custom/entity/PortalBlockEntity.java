package net.nethredras.create_portals.block.custom.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PortalBlockEntity extends BlockEntity {
    private static final String TAG_LINKED_POS = "LinkedPortal";
    private static final String TAG_FACING = "Facing";

    @Nullable
    private BlockPos linkedPortal;
    private Direction facing = Direction.NORTH;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_BE.get(), pos, state);
    }

    // --- Linked portal ---
    @Nullable
    public BlockPos getLinkedPortal() {
        return linkedPortal;
    }

    public void setLinkedPortal(@Nullable BlockPos linkedPortal) {
        this.linkedPortal = linkedPortal;
        setChanged(); // marks the chunk dirty so it actually gets saved
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isLinked() {
        return linkedPortal != null;
    }

    public void unlink() {
        setLinkedPortal(null);
    }

    // --- Facing ---
    // Mirrors the block's own FACING property. Storing it here too is
    // convenient for teleport math (so you don't need to re-fetch the
    // BlockState every time), but always keep it in sync with the block.

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
        setChanged();
    }

    // --- Persistence ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedPortal != null) {
            tag.putLong(TAG_LINKED_POS, linkedPortal.asLong());
        }
        tag.putString(TAG_FACING, facing.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_LINKED_POS)) {
            linkedPortal = BlockPos.of(tag.getLong(TAG_LINKED_POS));
        } else {
            linkedPortal = null;
        }
        if (tag.contains(TAG_FACING)) {
            facing = Direction.byName(tag.getString(TAG_FACING));
            if (facing == null) {
                facing = Direction.NORTH;
            }
        }
    }

    // --- Client sync ---
    // Ensures the linked/unlinked state (and facing) reaches the client
    // whenever setChanged() + sendBlockUpdated() fires, e.g. for particle
    // or rendering differences between an active and inactive portal.

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}