package net.minecraftforge.gradle.mcp.naming;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.mcp.naming.tasks.ApplyMcpMappingsToSourceJar;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static net.minecraftforge.gradle.common.util.NamingConstants.Version.VERSION;

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

    private @NotNull TaskProvider<? extends Runtime> build(TaskBuildingContext context) {
        final String mappingVersion = context.getMappingVersion().get(VERSION);
        if (mappingVersion == null) {
            throw new IllegalStateException("Missing mapping version");
        }
        final TaskProvider<DownloadMavenArtifact> downloadMavenArtifact = buildMcpMappingsDownloadTask(context.spec().project(), context.getNamingChannel(), mappingVersion);
        final String applyTaskName = String.format("apply%s%sMappingsTo%s", StringUtils.capitalize(context.getNamingChannel().getName()), mappingVersion, StringUtils.capitalize(context.spec().name()));

        return context.spec().project().getTasks().register(applyTaskName, ApplyMcpMappingsToSourceJar.class, applyMcpMappingsToSourceJarTask -> {
            applyMcpMappingsToSourceJarTask.setGroup("ForgeGradle");
            applyMcpMappingsToSourceJarTask.setDescription(String.format("Applies the MCP mappings for version %s to %s pipeline", mappingVersion, context.spec().name()));

            applyMcpMappingsToSourceJarTask.getMappings().set(downloadMavenArtifact.flatMap(DownloadMavenArtifact::getOutput));
            applyMcpMappingsToSourceJarTask.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
            applyMcpMappingsToSourceJarTask.getStepName().set("applyMcpMappings");
        });
    }
}
