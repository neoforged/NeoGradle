package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic;

/**
 * Defines the distribution type (also known as the side) of a game artifact.
 */
@CompileStatic
enum DistributionType {
    /**
     * Defines the client distribution type, generally contains the game code, including rendering logic and all its assets.
     */
    CLIENT("client", GameArtifact.CLIENT_JAR, true, false),
    /**
     * Defines the server distribution type, generally contains only the game logic code and game logic (data) assets.
     */
    SERVER("server", GameArtifact.SERVER_JAR, false, true),
    /**
     * Defines the common distribution type, is a merged version of client and server distribution types, generally the client overrides the server.
     */
    JOINED("joined", GameArtifact.CLIENT_JAR, true, true);

    private final String side;
    private final GameArtifact gameArtifact;
    private final boolean client;
    private final boolean server;

    DistributionType(String side, GameArtifact gameArtifact, boolean client, boolean server) {
        this.side = side;
        this.gameArtifact = gameArtifact;
        this.client = client;
        this.server = server;
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

    /**
     * Indicates if this distribution type contains client code.
     *
     * @return {@code true} if this distribution type contains client code, {@code false} otherwise.
     */
    public boolean isClient() {
        return client
    }

    /**
     * Indicates if this distribution type contains server code.
     *
     * @return {@code true} if this distribution type contains server code, {@code false} otherwise.
     */
    public boolean isServer() {
        return server
    }

}
