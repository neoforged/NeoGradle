package net.minecraftforge.gradle.common.util;

import java.util.function.Function;
import java.util.function.Predicate;

public enum GameArtifact {
    LAUNCHER_MANIFEST(version -> ICacheFileSelector.launcherMetadata(), (side) -> true),
    VERSION_MANIFEST(ICacheFileSelector::forVersionJson, (side) -> true),
    CLIENT_JAR(version -> ICacheFileSelector.forVersionJar(version, "client"), (side) -> side != ArtifactSide.SERVER),
    SERVER_JAR(version -> ICacheFileSelector.forVersionJar(version, "server"), (side) -> side != ArtifactSide.CLIENT),
    CLIENT_MAPPINGS(version -> ICacheFileSelector.forVersionMappings(version, "client"), (side) -> true),
    SERVER_MAPPINGS(version -> ICacheFileSelector.forVersionMappings(version, "server"), (side) -> true);

    private final Function<String, ICacheFileSelector> selectorBuilder;
    private final Predicate<ArtifactSide> isRequiredForSide;

    GameArtifact(Function<String, ICacheFileSelector> selectorBuilder, Predicate<ArtifactSide> isRequiredForSide) {
        this.selectorBuilder = selectorBuilder;
        this.isRequiredForSide = isRequiredForSide;
    }

    public ICacheFileSelector getCacheSelectorForVersion(final String minecraftVersion) {
        return selectorBuilder.apply(minecraftVersion);
    }

    public boolean isRequiredForSide(final ArtifactSide side) {
        return isRequiredForSide.test(side);
    }
}
