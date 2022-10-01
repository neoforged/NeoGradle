package net.minecraftforge.gradle.mcp.naming;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact;
import net.minecraftforge.gradle.common.tasks.ForgeGradleBaseTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.tasks.ApplyMcpMappingsToSourceJar;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public final class MCPNamingChannelConfigurator {
    private static final MCPNamingChannelConfigurator INSTANCE = new MCPNamingChannelConfigurator();
    private static final Set<String> SUPPORTED_CHANNELS = ImmutableSet.of("snapshot", "snapshot_nodoc", "stable", "stable_nodoc");

    private MCPNamingChannelConfigurator() {
    }

    public static MCPNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    public void configure(final Project project) {
        final McpMinecraftExtension mcpMinecraftExtension = project.getExtensions().getByType(McpMinecraftExtension.class);
        SUPPORTED_CHANNELS.forEach(channelName -> mcpMinecraftExtension.getNamingChannelProviders().register(channelName, namingChannelProvider ->
                namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::buildMcpMappingsSourceJarRemappingTask)));
    }

    public TaskProvider<DownloadMavenArtifact> buildMcpMappingsDownloadTask(@NotNull final Project project, @NotNull final NamingChannelProvider namingChannelProvider, @NotNull final String mappingsVersion) {
        final String mcpDownloadTaskName = "downloadMcpMappings%s%s".formatted(namingChannelProvider.getName(), mappingsVersion);

        if (project.getTasks().findByName(mcpDownloadTaskName) != null) {
            return project.getTasks().named(mcpDownloadTaskName, DownloadMavenArtifact.class);
        }

        return project.getTasks().register(mcpDownloadTaskName, DownloadMavenArtifact.class, downloadMavenArtifactTask -> {
            downloadMavenArtifactTask.setGroup("ForgeGradle");
            downloadMavenArtifactTask.setDescription("Downloads the MCP mappings for version %s".formatted(mappingsVersion));
            downloadMavenArtifactTask.setArtifact("de.oceanlabs.mcp:mcp_%s:%s@zip".formatted(namingChannelProvider.getName(), mappingsVersion));
        });
    }

    public <I extends ForgeGradleBaseTask & ITaskWithOutput> @NotNull TaskProvider<?> buildMcpMappingsSourceJarRemappingTask(@NotNull final Project project, @NotNull final NamingChannelProvider namingChannelProvider, @NotNull final Map<String, String> mappingVersionData, @NotNull final TaskProvider<I> taskOutputToModify) {
        final String mappingVersion = mappingVersionData.computeIfAbsent(McpRuntimeConstants.Naming.Version.VERSION, versionKey -> {
            throw new IllegalStateException("Missing version from mapping version data");
        });
        final TaskProvider<DownloadMavenArtifact> downloadMavenArtifact = buildMcpMappingsDownloadTask(project, namingChannelProvider, mappingVersion);
        final String applyTaskName = "apply%s%sMappingsTo%s".formatted(StringUtils.capitalize(namingChannelProvider.getName()), mappingVersion, taskOutputToModify.getName());

        return project.getTasks().register(applyTaskName, ApplyMcpMappingsToSourceJar.class, applyMcpMappingsToSourceJarTask -> {
            applyMcpMappingsToSourceJarTask.setGroup("ForgeGradle");
            applyMcpMappingsToSourceJarTask.setDescription("Applies the MCP mappings for version %s to the %s task".formatted(mappingVersion, taskOutputToModify.getName()));

            applyMcpMappingsToSourceJarTask.getMappings().set(downloadMavenArtifact.flatMap(DownloadMavenArtifact::getOutput));
            applyMcpMappingsToSourceJarTask.getInput().set(taskOutputToModify.flatMap(ITaskWithOutput::getOutput));
        });
    }
}
