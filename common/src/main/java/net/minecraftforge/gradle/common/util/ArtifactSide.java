package net.minecraftforge.gradle.common.util;

public enum ArtifactSide {
    CLIENT("client", GameArtifact.CLIENT_JAR),
    SERVER("server", GameArtifact.SERVER_JAR),
    JOINED("joined", GameArtifact.CLIENT_JAR);

    private final String side;
    private final GameArtifact gameArtifact;

    ArtifactSide(String side, GameArtifact gameArtifact) {
        this.side = side;
        this.gameArtifact = gameArtifact;
    }

    public String getName() {
        return this.side;
    }

    public GameArtifact gameArtifact() {
        return gameArtifact;
    }
}
