package net.minecraftforge.gradle.mcp.naming;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import net.minecraftforge.gradle.common.runtime.naming.RenamingTaskBuildingContext;
import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.mcp.naming.tasks.ApplyMcpMappingsToSourceJar;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

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
        final MinecraftExtension minecraftExtension = project.getExtensions().getByType(MinecraftExtension.class);
        SUPPORTED_CHANNELS.forEach(channelName -> minecraftExtension.getNamingChannelProviders().register(channelName, namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::build);
            namingChannelProvider.getHasAcceptedLicense().set(true);
            namingChannelProvider.getLicenseText().set("MCP is licensed under the terms of the MCP license.");
        }));
    }

    public TaskProvider<DownloadMavenArtifact> buildMcpMappingsDownloadTask(@NotNull final Project project, @NotNull final NamingChannelProvider namingChannelProvider, @NotNull final String mappingsVersion) {
        final String mcpDownloadTaskName = String.format("downloadMcpMappings%s%s", namingChannelProvider.getName(), mappingsVersion);

        if (project.getTasks().findByName(mcpDownloadTaskName) != null) {
            return project.getTasks().named(mcpDownloadTaskName, DownloadMavenArtifact.class);
        }

        return project.getTasks().register(mcpDownloadTaskName, DownloadMavenArtifact.class, downloadMavenArtifactTask -> {
            downloadMavenArtifactTask.setGroup("ForgeGradle");
            downloadMavenArtifactTask.setDescription(String.format("Downloads the MCP mappings for version %s", mappingsVersion));
            downloadMavenArtifactTask.setArtifact(String.format("de.oceanlabs.mcp:mcp_%s:%s@zip", namingChannelProvider.getName(), mappingsVersion));
        });
    }

    private @NotNull TaskProvider<? extends IMcpRuntimeTask> build(RenamingTaskBuildingContext context) {
        final String mappingVersion = context.mappingVersionData().get(McpRuntimeConstants.Naming.Version.VERSION);
        if (mappingVersion == null) {
            throw new IllegalStateException("Missing mapping version");
        }
        final TaskProvider<DownloadMavenArtifact> downloadMavenArtifact = buildMcpMappingsDownloadTask(context.spec().project(), context.namingChannelProvider(), mappingVersion);
        final String applyTaskName = String.format("apply%s%sMappingsTo%s", StringUtils.capitalize(context.namingChannelProvider().getName()), mappingVersion, context.taskOutputToModify().getName());

        return context.spec().project().getTasks().register(applyTaskName, ApplyMcpMappingsToSourceJar.class, applyMcpMappingsToSourceJarTask -> {
            applyMcpMappingsToSourceJarTask.setGroup("ForgeGradle");
            applyMcpMappingsToSourceJarTask.setDescription(String.format("Applies the MCP mappings for version %s to the %s task", mappingVersion, context.taskOutputToModify().getName()));

            applyMcpMappingsToSourceJarTask.getMappings().set(downloadMavenArtifact.flatMap(DownloadMavenArtifact::getOutput));
            applyMcpMappingsToSourceJarTask.getInput().set(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutput));
            applyMcpMappingsToSourceJarTask.getStepName().set("applyMcpMappings");
        });
    }
}
