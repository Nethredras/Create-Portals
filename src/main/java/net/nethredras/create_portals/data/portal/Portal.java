package net.nethredras.create_portals.data.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class Portal {
    private UUID id;
    private BlockPos lowerPortalPos;
    private ResourceKey<Level> dimension;
    private Direction facing;
    private UUID linkedPortalId;

    // Constructor
    public Portal(UUID id, ResourceKey<Level> dimension, BlockPos lowerPortalPos, Direction facing) {
        this.id = id;
        this.dimension = dimension;
        this.lowerPortalPos = lowerPortalPos;
        this.facing = facing;
        this.linkedPortalId = null;
    }

    // Getter and Setter
    public UUID getId() {
        return id;
    }

    public BlockPos getLowerPortalPos() {
        return lowerPortalPos;
    }

    public BlockPos getUpperPortalPos() {
        return lowerPortalPos.above();
    }

    public void setLowerPortalPos(BlockPos lowerPortalPos) {
        this.lowerPortalPos = lowerPortalPos;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public UUID getLinkedPortalId() {
        return linkedPortalId;
    }

    public void setLinkedPortalId(UUID linkedPortalId) {
        this.linkedPortalId = linkedPortalId;
    }

    public boolean isLinked() {
        return linkedPortalId != null;
    }
}
