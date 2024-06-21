/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils

/**
 * Defines the distribution type (also known as the side) of a game artifact.
 */
@CompileStatic
enum DistributionType {
    /**
     * Defines the client distribution type, generally contains the game code, including rendering logic and all its assets.
     */
    CLIENT("client", true, false),
    /**
     * Defines the server distribution type, generally contains only the game logic code and game logic (data) assets.
     */
    SERVER("server", false, true),
    /**
     * Defines the common distribution type, is a merged version of client and server distribution types, generally the client overrides the server.
     */
    JOINED("joined", true, true);

    private final String side
    private final boolean client
    private final boolean server

    DistributionType(String side, boolean client, boolean server) {
        this.side = side
        this.client = client
        this.server = server
    }

    /**
     * Gets the distribution type identifier.
     *
     * @return The distribution type identifier.
     */
    String getName() {
        return this.side
    }

    /**
     * Gets the game artifact associated with this distribution type.
     *
     * @return The game artifact associated with this distribution type.
     */
    GameArtifact getGameArtifact() {
        return this == SERVER ? GameArtifact.SERVER_JAR : GameArtifact.CLIENT_JAR
    }

    /**
     * Indicates if this distribution type contains client code.
     *
     * @return {@code true} if this distribution type contains client code, {@code false} otherwise.
     */
    boolean isClient() {
        return client
    }

    /**
     * Indicates if this distribution type contains server code.
     *
     * @return {@code true} if this distribution type contains server code, {@code false} otherwise.
     */
    boolean isServer() {
        return server
    }

    /**
     * Creates a task identifier where the distribution type is sandwiched between the given prefix and suffix.
     *
     * @param prefix The prefix for the task identifier.
     * @param suffix The suffix for the task identifier.
     */
    String createTaskName(final String prefix, final String suffix) {
        return "${StringUtils.uncapitalize(prefix)}${StringUtils.capitalize(this.name().toLowerCase(Locale.ROOT))}${StringUtils.capitalize(suffix)}".toString();
    }
}
