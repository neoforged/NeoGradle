package net.minecraftforge.gradle.userdev.runtime;

import net.minecraftforge.gradle.configurations.UserDevConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.userdev.runtime.spec.ForgeUserDevRuntimeSpec;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.util.Objects;

/**
 * Represents a configured and registered runtime for forges userdev environment.
 */
public final class ForgeUserDevRuntimeDefinition {
    private final ForgeUserDevRuntimeSpec spec;
    private final McpRuntimeDefinition mcpRuntimeDefinition;
    private final File unpackedUserDevJarDirectory;
    private final UserDevConfigurationSpecV2 userdevConfiguration;
    private final Configuration additionalUserDevDependencies;

    public ForgeUserDevRuntimeDefinition(
            ForgeUserDevRuntimeSpec spec,
            McpRuntimeDefinition mcpRuntimeDefinition,
            File unpackedUserDevJarDirectory,
            UserDevConfigurationSpecV2 userdevConfiguration,
            Configuration additionalUserDevDependencies) {
        this.spec = spec;
        this.mcpRuntimeDefinition = mcpRuntimeDefinition;
        this.unpackedUserDevJarDirectory = unpackedUserDevJarDirectory;
        this.userdevConfiguration = userdevConfiguration;
        this.additionalUserDevDependencies = additionalUserDevDependencies;
    }

    public ForgeUserDevRuntimeSpec spec() {
        return spec;
    }

    public McpRuntimeDefinition mcpRuntimeDefinition() {
        return mcpRuntimeDefinition;
    }

    public File unpackedUserDevJarDirectory() {
        return unpackedUserDevJarDirectory;
    }

    public UserDevConfigurationSpecV2 userdevConfiguration() {
        return userdevConfiguration;
    }

    public Configuration additionalUserDevDependencies() {
        return additionalUserDevDependencies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ForgeUserDevRuntimeDefinition that = (ForgeUserDevRuntimeDefinition) obj;
        return Objects.equals(this.spec, that.spec) &&
                Objects.equals(this.mcpRuntimeDefinition, that.mcpRuntimeDefinition) &&
                Objects.equals(this.unpackedUserDevJarDirectory, that.unpackedUserDevJarDirectory) &&
                Objects.equals(this.userdevConfiguration, that.userdevConfiguration) &&
                Objects.equals(this.additionalUserDevDependencies, that.additionalUserDevDependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, mcpRuntimeDefinition, unpackedUserDevJarDirectory, userdevConfiguration, additionalUserDevDependencies);
    }

    @Override
    public String toString() {
        return "ForgeUserDevRuntimeDefinition[" +
                "spec=" + spec + ", " +
                "mcpRuntimeDefinition=" + mcpRuntimeDefinition + ", " +
                "unpackedUserDevJarDirectory=" + unpackedUserDevJarDirectory + ", " +
                "userdevConfiguration=" + userdevConfiguration + ", " +
                "additionalUserDevDependencies=" + additionalUserDevDependencies + ']';
    }

}
