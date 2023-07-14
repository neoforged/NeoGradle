package net.minecraftforge.gradle.mcp.runtime.definition;

import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.tasks.ClientExtraJar;
import net.minecraftforge.gradle.common.runtime.tasks.DownloadAssets;
import net.minecraftforge.gradle.common.runtime.tasks.ExtractNatives;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.dsl.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.dsl.mcp.runtime.definition.McpDefinition;
import net.minecraftforge.gradle.mcp.runtime.specification.McpRuntimeSpecification;
import net.minecraftforge.gradle.common.runs.run.RunImpl;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a configured and registered runtime for Mcp.
 */
public class McpRuntimeDefinition extends CommonRuntimeDefinition<McpRuntimeSpecification> implements McpDefinition<McpRuntimeSpecification> {
    private final File unpackedMcpZipDirectory;
    private final McpConfigConfigurationSpecV2 mcpConfig;

    private final TaskProvider<ClientExtraJar> clientExtraJarProvider;
    private final TaskProvider<DownloadAssets> assetsTaskProvider;
    private final TaskProvider<ExtractNatives> nativesTaskProvider;
    private TaskProvider<? extends WithOutput> debuggingMappingsTaskProvider;

    public McpRuntimeDefinition(@NotNull McpRuntimeSpecification specification,
                                @NotNull LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs,
                                @NotNull TaskProvider<? extends ArtifactProvider> sourceJarTask,
                                @NotNull TaskProvider<? extends ArtifactProvider> rawJarTask,
                                @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks,
                                @NotNull Configuration minecraftDependenciesConfiguration,
                                @NotNull Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer,
                                @NotNull File unpackedMcpZipDirectory,
                                @NotNull McpConfigConfigurationSpecV2 mcpConfig,
                                @NotNull TaskProvider<ClientExtraJar> clientExtraJarProvider,
                                @NotNull TaskProvider<DownloadAssets> assetsTaskProvider,
                                @NotNull TaskProvider<ExtractNatives> nativesTaskProvider) {
        super(specification, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration, associatedTaskConsumer);
        this.unpackedMcpZipDirectory = unpackedMcpZipDirectory;
        this.mcpConfig = mcpConfig;
        this.clientExtraJarProvider = clientExtraJarProvider;
        this.assetsTaskProvider = assetsTaskProvider;
        this.nativesTaskProvider = nativesTaskProvider;
    }


    @Override
    @NotNull
    public File getUnpackedMcpZipDirectory() {
        return unpackedMcpZipDirectory;
    }

    @Override
    @NotNull
    public McpConfigConfigurationSpecV2 getMcpConfig() {
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

    public TaskProvider<ClientExtraJar> getClientExtraJarProvider() {
        return clientExtraJarProvider;
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssetsTaskProvider() {
        return assetsTaskProvider;
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNativesTaskProvider() {
        return nativesTaskProvider;
    }

    public @NotNull TaskProvider<? extends WithOutput> getDebuggingMappingsTaskProvider() {
        return debuggingMappingsTaskProvider;
    }

    public void setDebuggingMappingsTaskProvider(TaskProvider<? extends WithOutput> debuggingMappingsTaskProvider) {
        this.debuggingMappingsTaskProvider = debuggingMappingsTaskProvider;
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
        run.getClasspath().from(this.getDebuggingMappingsTaskProvider());
    }

    @Override
    public Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = new HashMap<>(super.buildRunInterpolationData());

        interpolationData.put("mcp_version", mcpConfig.getVersion());
        interpolationData.put("mcp_mappings", new File(unpackedMcpZipDirectory, "config/joined.srg").getAbsolutePath());

        return interpolationData;
    }

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return getTask("listLibraries");
    }
}
