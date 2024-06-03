/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic

import java.util.function.Function
import java.util.function.Predicate

@CompileStatic
enum GameArtifact {
    VERSION_MANIFEST((String version) -> CacheFileSelector.forVersionJson(version), (type) -> true, Optional.<MinecraftArtifactType> empty(), Optional.<DistributionType>empty()),
    CLIENT_JAR((String version) -> CacheFileSelector.forVersionJar(version, "client"), (type) -> type != DistributionType.SERVER, Optional.of(MinecraftArtifactType.EXECUTABLE), Optional.of(DistributionType.CLIENT)),
    SERVER_JAR((String version) -> CacheFileSelector.forVersionJar(version, "server"), (type) -> type != DistributionType.CLIENT, Optional.of(MinecraftArtifactType.EXECUTABLE), Optional.of(DistributionType.SERVER)),
    CLIENT_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "client"), (type) -> true, Optional.of(MinecraftArtifactType.MAPPINGS), Optional.of(DistributionType.CLIENT)),
    SERVER_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "server"), (type) -> true, Optional.of(MinecraftArtifactType.MAPPINGS), Optional.of(DistributionType.SERVER));

    private final Function<String, CacheFileSelector> selectorBuilder;
    private final Predicate<DistributionType> isRequiredForSide;
    private final Optional<MinecraftArtifactType> artifactType;
    private final Optional<DistributionType> distributionType;

    GameArtifact(Function<String, CacheFileSelector> selectorBuilder, Predicate<DistributionType> isRequiredForSide, Optional<MinecraftArtifactType> artifactType, Optional<DistributionType> distributionType) {
        this.selectorBuilder = selectorBuilder;
        this.isRequiredForSide = isRequiredForSide;
        this.artifactType = artifactType
        this.distributionType = distributionType
    }

    CacheFileSelector getCacheSelectorForVersion(final String minecraftVersion) {
        return selectorBuilder.apply(minecraftVersion);
    }

    boolean isRequiredForDistribution(final DistributionType side) {
        return isRequiredForSide.test(side);
    }

    void doWhenRequired(DistributionType side, Runnable o) {
        if (isRequiredForDistribution(side))
            o.run();
    }

    Optional<MinecraftArtifactType> getType() {
        return artifactType
    }

    Optional<DistributionType> getDistributionType() {
        return distributionType
    }
}
