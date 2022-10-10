package net.minecraftforge.gradle.common.util;

public enum ArtifactSide {
    CLIENT("client"),
    SERVER("server"),
    JOINED("joined");

    private final String side;

    ArtifactSide(String side) {
        this.side = side;
    }

    public String getName() {
        return this.side;
    }
}
