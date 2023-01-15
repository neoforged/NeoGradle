package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic

import java.util.function.Function
import java.util.function.Predicate

@CompileStatic
enum GameArtifact {
    LAUNCHER_MANIFEST((String version) -> CacheFileSelector.launcherMetadata(), (side) -> true),
    VERSION_MANIFEST(CacheFileSelector.&forVersionJson, (DistributionType side) -> true),
    CLIENT_JAR((String version) -> CacheFileSelector.forVersionJar(version, "client"), (side) -> side != DistributionType.SERVER),
    SERVER_JAR((String version) -> CacheFileSelector.forVersionJar(version, "server"), (side) -> side != DistributionType.CLIENT),
    CLIENT_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "client"), (side) -> true),
    SERVER_MAPPINGS((String version) -> CacheFileSelector.forVersionMappings(version, "server"), (side) -> true);

    private final Function<String, CacheFileSelector> selectorBuilder;
    private final Predicate<DistributionType> isRequiredForSide;

    GameArtifact(Function<String, CacheFileSelector> selectorBuilder, Predicate<DistributionType> isRequiredForSide) {
        this.selectorBuilder = selectorBuilder;
        this.isRequiredForSide = isRequiredForSide;
    }

    CacheFileSelector getCacheSelectorForVersion(final String minecraftVersion) {
        return selectorBuilder.apply(minecraftVersion);
    }

    boolean isRequiredForSide(final DistributionType side) {
        return isRequiredForSide.test(side);
    }
}
