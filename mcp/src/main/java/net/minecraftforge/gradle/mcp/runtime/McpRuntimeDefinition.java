package net.minecraftforge.gradle.mcp.runtime;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a configured and registered runtime for Mcp.
 */
public class McpRuntimeDefinition extends CommonRuntimeDefinition<McpRuntimeSpec> {
    private final File unpackedMcpZipDirectory;
    private final McpConfigConfigurationSpecV2 mcpConfig;

    public McpRuntimeDefinition(
            McpRuntimeSpec spec,
            LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs,
            File unpackedMcpZipDirectory,
            McpConfigConfigurationSpecV2 mcpConfig,
            TaskProvider<? extends ArtifactProvider> sourceJarTask,
            TaskProvider<? extends ArtifactProvider> rawJarTask,
            Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks,
            Map<GameArtifact, File> gameArtifacts,
            Configuration minecraftDependenciesConfiguration) {
        super(spec, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, gameArtifacts, minecraftDependenciesConfiguration);
        this.unpackedMcpZipDirectory = unpackedMcpZipDirectory;
        this.mcpConfig = mcpConfig;
    }

    public File unpackedMcpZipDirectory() {
        return unpackedMcpZipDirectory;
    }

    public McpConfigConfigurationSpecV2 mcpConfig() {
        return mcpConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpRuntimeDefinition)) return false;
        if (!super.equals(o)) return false;

        McpRuntimeDefinition that = (McpRuntimeDefinition) o;

        if (!unpackedMcpZipDirectory.equals(that.unpackedMcpZipDirectory)) return false;
        return mcpConfig.equals(that.mcpConfig);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + unpackedMcpZipDirectory.hashCode();
        result = 31 * result + mcpConfig.hashCode();
        return result;
    }
}
