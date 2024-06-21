package net.neoforged.gradle.dsl.common.util;

public enum MinecraftArtifactType {

    VERSION_JSON("version.json"),
    EXECUTABLE("%s"),
    MAPPINGS("%s_mappings");
    
    private final String identifierFormat;
    
    MinecraftArtifactType(String identifierFormat) {
        this.identifierFormat = identifierFormat;
    }
    
    public String createIdentifier(DistributionType distributionType) {
        return String.format(identifierFormat, distributionType.getName());
    }
}
