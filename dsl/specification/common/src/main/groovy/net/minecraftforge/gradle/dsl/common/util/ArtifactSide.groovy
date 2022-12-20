package net.minecraftforge.gradle.dsl.common.util;

/**
 * Defines the distribution type (also known as the side) of a game artifact.
 */
public enum ArtifactSide {
    /**
     * Defines the client distribution type, generally contains the game code, including rendering logic and all its assets.
     */
    CLIENT("client", GameArtifact.CLIENT_JAR),
    /**
     * Defines the server distribution type, generally contains only the game logic code and game logic (data) assets.
     */
    SERVER("server", GameArtifact.SERVER_JAR),
    /**
     * Defines the common distribution type, is a merged version of client and server distribution types, generally the client overrides the server.
     */
    JOINED("joined", GameArtifact.CLIENT_JAR);

    private final String side;
    private final GameArtifact gameArtifact;

    ArtifactSide(String side, GameArtifact gameArtifact) {
        this.side = side;
        this.gameArtifact = gameArtifact;
    }

    /**
     * Gets the distribution type name.
     *
     * @return The distribution type name.
     */
    public String getName() {
        return this.side;
    }

    /**
     * Gets the game artifact associated with this distribution type.
     *
     * @return The game artifact associated with this distribution type.
     */
    public GameArtifact getGameArtifact() {
        return gameArtifact;
    }
}
