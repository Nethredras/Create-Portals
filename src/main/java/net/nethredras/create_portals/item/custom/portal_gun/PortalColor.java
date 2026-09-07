package net.nethredras.create_portals.item.custom.portal_gun;

import net.minecraft.util.StringRepresentable;

public enum PortalColor implements StringRepresentable {
    BLUE("blue"),
    ORANGE("orange");

    private final String name;

    PortalColor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
