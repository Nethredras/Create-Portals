package net.nethredras.create_portals.block.custom.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PortalBlockEntity extends BlockEntity {
    private static final String TAG_LINKED_POS = "LinkedPortal";
    private static final String TAG_FACING = "Facing";

    private @Nullable GlobalPos linkedPortal;
    private Direction facing = Direction.NORTH;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_BE.get(), pos, state);
    }

    // --- Linked portal ---
    public @Nullable GlobalPos getLinkedPortal() {
        return linkedPortal;
    }

    public void setLinkedPortal(@Nullable GlobalPos linkedPortal) {
        this.linkedPortal = linkedPortal;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);

            BlockPos wallPos = getBlockPos().relative(facing.getOpposite());
            if (level.isLoaded(wallPos)) {
                BlockState wallState = level.getBlockState(wallPos);
                level.sendBlockUpdated(wallPos, wallState, wallState, 3);
            }
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
            GlobalPos.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), linkedPortal)
                    .resultOrPartial(err -> {}) // Silently drops error
                    .ifPresent(encoded -> tag.put(TAG_LINKED_POS, encoded));
        }
        tag.putString(TAG_FACING, facing.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_LINKED_POS)) {
            Tag posTag = tag.get(TAG_LINKED_POS);
            linkedPortal = GlobalPos.CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), posTag)
                    .resultOrPartial(err -> {})
                    .orElse(null);
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