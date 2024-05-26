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
    VERSION_MANIFEST((String version) -> CacheFileSelector.forVersionJson(version), (type) -> true),
    CLIENT_JAR((String version) -> CacheFileSelector.forVersionJar(version, "client"), (type) -> type != DistributionType.SERVER),
    SERVER_JAR((String version) -> CacheFileSelector.forVersionJar(version, "server"), (type) -> type != DistributionType.CLIENT),
    CLIENT_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "client"), (type) -> true),
    SERVER_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "server"), (type) -> true);

    private final Function<String, CacheFileSelector> selectorBuilder;
    private final Predicate<DistributionType> isRequiredForSide;

    GameArtifact(Function<String, CacheFileSelector> selectorBuilder, Predicate<DistributionType> isRequiredForSide) {
        this.selectorBuilder = selectorBuilder;
        this.isRequiredForSide = isRequiredForSide;
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
}
