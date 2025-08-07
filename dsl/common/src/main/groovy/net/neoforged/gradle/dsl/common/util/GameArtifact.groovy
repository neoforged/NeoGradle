/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic

import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

@CompileStatic
enum GameArtifact {
    VERSION_MANIFEST((String version) -> CacheFileSelector.forVersionJson(version), (type) -> true, () -> DistributionType.JOINED, MinecraftArtifactType.METADATA),
    CLIENT_JAR(
            (String version) -> CacheFileSelector.forVersionJar(version, "client"),
            (type) -> type != DistributionType.SERVER,
            () -> DistributionType.CLIENT,
            MinecraftArtifactType.EXECUTABLE
    ),
    SERVER_JAR((String version) -> CacheFileSelector.forVersionJar(version, "server"), (type) -> type != DistributionType.CLIENT, () -> DistributionType.SERVER, MinecraftArtifactType.EXECUTABLE),
    EXTRACTED_SERVER_JAR((String version) -> CacheFileSelector.forVersionJar(version, "server_extracted"), (type) -> type != DistributionType.CLIENT, () -> DistributionType.SERVER, MinecraftArtifactType.EXTRACTED),
    CLIENT_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "client"), (type) -> true, () -> DistributionType.CLIENT, MinecraftArtifactType.MAPPINGS),
    SERVER_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "server"), (type) -> true, () -> DistributionType.SERVER, MinecraftArtifactType.MAPPINGS);

    private final Function<String, CacheFileSelector> selectorBuilder;
    private final Predicate<DistributionType> isRequiredForSide;
    private final Supplier<DistributionType> distributionType;
    private final MinecraftArtifactType minecraftArtifactType;

    GameArtifact(
            Function<String, CacheFileSelector> selectorBuilder,
            Predicate<DistributionType> isRequiredForSide,
            Supplier<DistributionType> distType,
            MinecraftArtifactType artifactType) {
        this.selectorBuilder = selectorBuilder;
        this.isRequiredForSide = isRequiredForSide;
        this.distributionType = distType
        this.minecraftArtifactType = artifactType
    }

    CacheFileSelector getCacheSelectorForVersion(final String minecraftVersion) {
        return this.selectorBuilder.apply(minecraftVersion);
    }

    boolean isRequiredForDistribution(final DistributionType side) {
        return this.isRequiredForSide.test(side);
    }

    void doWhenRequired(DistributionType side, Runnable o) {
        if (isRequiredForDistribution(side))
            o.run();
    }

    DistributionType getDistribution() {
        return this.distributionType.get()
    }

    MinecraftArtifactType getMinecraftArtifact() {
        return this.minecraftArtifactType
    }
}
