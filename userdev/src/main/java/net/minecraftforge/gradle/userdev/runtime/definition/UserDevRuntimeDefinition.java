package net.minecraftforge.gradle.userdev.runtime.definition;

import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.minecraftforge.gradle.dsl.userdev.runtime.definition.UserDevDefinition;
import net.minecraftforge.gradle.mcp.runtime.definition.McpRuntimeDefinition;
import net.minecraftforge.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents a configured and registered runtime for forges userdev environment.
 */
public final class UserDevRuntimeDefinition extends CommonRuntimeDefinition<UserDevRuntimeSpecification> implements UserDevDefinition<UserDevRuntimeSpecification> {
    private final McpRuntimeDefinition mcpRuntimeDefinition;
    private final File unpackedUserDevJarDirectory;
    private final UserDevConfigurationSpecV2 userdevConfiguration;
    private final Configuration additionalUserDevDependencies;

    public UserDevRuntimeDefinition(@NotNull UserDevRuntimeSpecification specification, McpRuntimeDefinition mcpRuntimeDefinition, File unpackedUserDevJarDirectory, UserDevConfigurationSpecV2 userdevConfiguration, Configuration additionalUserDevDependencies) {
        super(specification, mcpRuntimeDefinition.getTasks(), mcpRuntimeDefinition.getSourceJarTask(), mcpRuntimeDefinition.getRawJarTask(), mcpRuntimeDefinition.getGameArtifactProvidingTasks(), mcpRuntimeDefinition.getMinecraftDependenciesConfiguration(), mcpRuntimeDefinition::configureAssociatedTask);
        this.mcpRuntimeDefinition = mcpRuntimeDefinition;
        this.unpackedUserDevJarDirectory = unpackedUserDevJarDirectory;
        this.userdevConfiguration = userdevConfiguration;
        this.additionalUserDevDependencies = additionalUserDevDependencies;
    }

    @Override
    public McpRuntimeDefinition getMcpRuntimeDefinition() {
        return mcpRuntimeDefinition;
    }

    @Override
    public File getUnpackedUserDevJarDirectory() {
        return unpackedUserDevJarDirectory;
    }

    @Override
    public UserDevConfigurationSpecV2 getUserdevConfiguration() {
        return userdevConfiguration;
    }

    @Override
    public Configuration getAdditionalUserDevDependencies() {
        return additionalUserDevDependencies;
    }

    @Override
    public void setReplacedDependency(@NotNull Dependency dependency) {
        super.setReplacedDependency(dependency);
        mcpRuntimeDefinition.setReplacedDependency(dependency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDevRuntimeDefinition)) return false;

        UserDevRuntimeDefinition that = (UserDevRuntimeDefinition) o;

        if (getMcpRuntimeDefinition() != null ? !getMcpRuntimeDefinition().equals(that.getMcpRuntimeDefinition()) : that.getMcpRuntimeDefinition() != null)
            return false;
        if (getUnpackedUserDevJarDirectory() != null ? !getUnpackedUserDevJarDirectory().equals(that.getUnpackedUserDevJarDirectory()) : that.getUnpackedUserDevJarDirectory() != null)
            return false;
        if (getUserdevConfiguration() != null ? !getUserdevConfiguration().equals(that.getUserdevConfiguration()) : that.getUserdevConfiguration() != null)
            return false;
        return getAdditionalUserDevDependencies() != null ? getAdditionalUserDevDependencies().equals(that.getAdditionalUserDevDependencies()) : that.getAdditionalUserDevDependencies() == null;
    }

    @Override
    public int hashCode() {
        int result = getMcpRuntimeDefinition() != null ? getMcpRuntimeDefinition().hashCode() : 0;
        result = 31 * result + (getUnpackedUserDevJarDirectory() != null ? getUnpackedUserDevJarDirectory().hashCode() : 0);
        result = 31 * result + (getUserdevConfiguration() != null ? getUserdevConfiguration().hashCode() : 0);
        result = 31 * result + (getAdditionalUserDevDependencies() != null ? getAdditionalUserDevDependencies().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserDevRuntimeDefinition{" +
                "mcpRuntimeDefinition=" + mcpRuntimeDefinition +
                ", unpackedUserDevJarDirectory=" + unpackedUserDevJarDirectory +
                ", userdevConfiguration=" + userdevConfiguration +
                ", additionalUserDevDependencies=" + additionalUserDevDependencies +
                '}';
    }
}
