package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact;
import net.minecraftforge.gradle.common.tasks.ForgeGradleBaseTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.tasks.ApplyMcpMappingsToSourceJar;
import net.minecraftforge.gradle.mcp.tasks.ApplyOfficialMappingsToSourceJar;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class OfficialNamingChannelConfigurator {
    private static final OfficialNamingChannelConfigurator INSTANCE = new OfficialNamingChannelConfigurator();

    public static OfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    private OfficialNamingChannelConfigurator() {
    }

    public void configure(final Project project) {
        final McpMinecraftExtension mcpMinecraftExtension = project.getExtensions().getByType(McpMinecraftExtension.class);
        mcpMinecraftExtension.getNamingChannelProviders().register("official", namingChannelProvider -> {

        });
    }

    @NotNull
    public TaskProvider<DownloadMavenArtifact> buildClientMappingsDownloadTask(@NotNull final Project project, @NotNull final String mappingVersion) {
        return buildMappingsDownloadTask("Client", mappingVersion, project);
    }

    @NotNull
    public TaskProvider<DownloadMavenArtifact> buildServerMappingsDownloadTask(@NotNull final Project project, @NotNull final String mappingVersion) {
        return buildMappingsDownloadTask("Server", mappingVersion, project);
    }

    @NotNull
    private static TaskProvider<DownloadMavenArtifact> buildMappingsDownloadTask(String type, @NotNull String mappingVersion, @NotNull Project project) {
        final String downloadServerMappingsTask = "download%sMappings%s".formatted(type, mappingVersion);

        if (project.getTasks().findByName(downloadServerMappingsTask) != null) {
            return project.getTasks().named(downloadServerMappingsTask, DownloadMavenArtifact.class);
        }

        return project.getTasks().register(downloadServerMappingsTask, DownloadMavenArtifact.class, downloadMavenArtifactTask -> {
            downloadMavenArtifactTask.setGroup("ForgeGradle");
            downloadMavenArtifactTask.setDescription("Downloads the Official %s mappings for version %s".formatted(type.toLowerCase(), mappingVersion));
            downloadMavenArtifactTask.setArtifact("net.minecraft:%s:%s:mappings@txt".formatted(type.toLowerCase(), mappingVersion));
        });
    }

    public <I extends ForgeGradleBaseTask & ITaskWithOutput> @NotNull TaskProvider<?> buildOfficialMappingsSourceJarRemappingTask(@NotNull final Project project, @NotNull final NamingChannelProvider namingChannelProvider, @NotNull final Map<String, String> mappingVersionData, @NotNull final TaskProvider<I> taskOutputToModify) {
        final String mappingVersion = mappingVersionData.computeIfAbsent(McpRuntimeConstants.Naming.Version.VERSION, versionKey -> {
            throw new IllegalStateException("Missing version from mapping version data");
        });
        final TaskProvider<DownloadMavenArtifact> clientMappingsDownload = buildClientMappingsDownloadTask(project, mappingVersion);
        final TaskProvider<DownloadMavenArtifact> serverMappingsDownload = buildServerMappingsDownloadTask(project, mappingVersion);

        final String applyTaskName = "apply%s%sMappingsTo%s".formatted(StringUtils.capitalize(namingChannelProvider.getName()), mappingVersion, taskOutputToModify.getName());

        return project.getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("ForgeGradle");
            applyOfficialMappingsToSourceJar.setDescription("Applies the Official mappings for version %s to the %s task".formatted(mappingVersion, taskOutputToModify.getName()));

            applyOfficialMappingsToSourceJar.getClientMappings().set(clientMappingsDownload.flatMap(DownloadMavenArtifact::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappings().set(serverMappingsDownload.flatMap(DownloadMavenArtifact::getOutput));

            if (mappingVersionData.containsKey(McpRuntimeConstants.Naming.Version.MCP_RUNTIME)) {
                applyOfficialMappingsToSourceJar.getMcpRuntimeName().set(mappingVersionData.get("mcpRuntime"));
            }

            applyOfficialMappingsToSourceJar.getInput().set(taskOutputToModify.flatMap(ITaskWithOutput::getOutput));
        });
    }
}
