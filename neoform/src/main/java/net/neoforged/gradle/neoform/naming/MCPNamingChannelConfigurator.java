package net.neoforged.gradle.neoform.naming;

import com.google.common.collect.ImmutableSet;
import net.neoforged.gradle.common.tasks.DownloadMavenArtifact;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.neoform.naming.tasks.ApplyNeoFormMappingsToSourceJar;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static net.neoforged.gradle.dsl.common.util.NamingConstants.Version.VERSION;

/**
 * Legacy naming channel configurator for the legacy Naming channel
 * This class exists so that legacy wrapper plugins can easily inject it.
 */
@Deprecated()
public final class MCPNamingChannelConfigurator {
    private static final MCPNamingChannelConfigurator INSTANCE = new MCPNamingChannelConfigurator();
    private static final Set<String> SUPPORTED_CHANNELS = ImmutableSet.of("snapshot", "snapshot_nodoc", "stable", "stable_nodoc");

    private MCPNamingChannelConfigurator() {
    }

    public static MCPNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        SUPPORTED_CHANNELS.forEach(channelName -> minecraftExtension.getNamingChannels().register(channelName, namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::build);
            namingChannelProvider.getHasAcceptedLicense().set(true);
            namingChannelProvider.getLicenseText().set("MCP is licensed under the terms of the MCP license.");
        }));
    }

    public TaskProvider<DownloadMavenArtifact> buildMcpMappingsDownloadTask(@NotNull final Project project, @NotNull final NamingChannel namingChannelProvider, @NotNull final String mappingsVersion) {
        final String mcpDownloadTaskName = String.format("downloadMcpMappings%s%s", namingChannelProvider.getName(), mappingsVersion);

        if (project.getTasks().findByName(mcpDownloadTaskName) != null) {
            return project.getTasks().named(mcpDownloadTaskName, DownloadMavenArtifact.class);
        }

        return project.getTasks().register(mcpDownloadTaskName, DownloadMavenArtifact.class, downloadMavenArtifactTask -> {
            downloadMavenArtifactTask.setDescription(String.format("Downloads the MCP mappings for version %s", mappingsVersion));
            downloadMavenArtifactTask.setArtifact(String.format("de.oceanlabs.mcp:mcp_%s:%s@zip", namingChannelProvider.getName(), mappingsVersion));
        });
    }

    private @NotNull TaskProvider<? extends Runtime> build(TaskBuildingContext context) {
        final String mappingVersion = context.getMappingVersion().get(VERSION);
        if (mappingVersion == null) {
            throw new IllegalStateException("Missing mapping version");
        }
        final TaskProvider<DownloadMavenArtifact> downloadMavenArtifact = buildMcpMappingsDownloadTask(context.getProject(), context.getNamingChannel(), mappingVersion);
        final String applyTaskName = context.getTaskNameBuilder().apply(
                String.format("apply%s%sMappings", StringUtils.capitalize(context.getNamingChannel().getName()), mappingVersion)
        );

        return context.getProject().getTasks().register(applyTaskName, ApplyNeoFormMappingsToSourceJar.class, applyNeoFormMappingsToSourceJarTask -> {
            applyNeoFormMappingsToSourceJarTask.setDescription(String.format("Applies the MCP mappings for version %s to %s pipeline", mappingVersion, context.getEnvironmentName()));

            applyNeoFormMappingsToSourceJarTask.getMappings().set(downloadMavenArtifact.flatMap(DownloadMavenArtifact::getOutput));
            applyNeoFormMappingsToSourceJarTask.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
            applyNeoFormMappingsToSourceJarTask.getStepName().set("applyMcpMappings");
        });
    }
}
