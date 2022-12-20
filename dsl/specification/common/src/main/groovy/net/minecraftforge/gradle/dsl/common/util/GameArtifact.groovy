package net.minecraftforge.gradle.dsl.common.util;

import java.util.function.Function;
import java.util.function.Predicate;

public enum GameArtifact {
    LAUNCHER_MANIFEST((version) -> CacheFileSelector.launcherMetadata(), (side) -> true),
    VERSION_MANIFEST(CacheFileSelector::forVersionJson, (side) -> true),
    CLIENT_JAR((version) -> CacheFileSelector.forVersionJar(version, "client"), (side) -> side != ArtifactSide.SERVER),
    SERVER_JAR((version) -> CacheFileSelector.forVersionJar(version, "server"), (side) -> side != ArtifactSide.CLIENT),
    CLIENT_MAPPINGS((version) -> CacheFileSelector.forVersionMappings(version, "client"), (side) -> true),
    SERVER_MAPPINGS((version) -> CacheFileSelector.forVersionMappings(version, "server"), (side) -> true);

    private final Function<String, CacheFileSelector> selectorBuilder;
    private final Predicate<ArtifactSide> isRequiredForSide;

    GameArtifact(Function<String, CacheFileSelector> selectorBuilder, Predicate<ArtifactSide> isRequiredForSide) {
        this.selectorBuilder = selectorBuilder;
        this.isRequiredForSide = isRequiredForSide;
    }

    public CacheFileSelector getCacheSelectorForVersion(final String minecraftVersion) {
        return selectorBuilder.apply(minecraftVersion);
    }

    public boolean isRequiredForSide(final ArtifactSide side) {
        return isRequiredForSide.test(side);
    }
}
