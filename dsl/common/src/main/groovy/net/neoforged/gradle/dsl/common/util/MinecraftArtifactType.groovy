package net.neoforged.gradle.dsl.common.util

enum MinecraftArtifactType {
    EXECUTABLE("%s"),
    MAPPINGS("%s_mappings"),
    EXTRACTED("%s_server"),
    METADATA("%s_metadata");

    private final String identifierFormat;

    MinecraftArtifactType(String identifierFormat) {
        this.identifierFormat = identifierFormat;
    }

    String createIdentifier(DistributionType distributionType) {
        return String.format(identifierFormat, distributionType.getName());
    }

}