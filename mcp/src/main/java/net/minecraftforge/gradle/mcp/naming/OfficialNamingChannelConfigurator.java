package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.common.util.ICacheFileSelector;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.runtime.tasks.DownloadCore;
import net.minecraftforge.gradle.mcp.runtime.tasks.FileCacheProviding;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import net.minecraftforge.gradle.mcp.tasks.ApplyOfficialMappingsToSourceJar;
import net.minecraftforge.gradle.mcp.util.MappingUtils;
import net.minecraftforge.gradle.mcp.util.McpConfigConstants;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public final class OfficialNamingChannelConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficialNamingChannelConfigurator.class);
    private static final OfficialNamingChannelConfigurator INSTANCE = new OfficialNamingChannelConfigurator();

    public static OfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    private OfficialNamingChannelConfigurator() {
    }

    public void configure(final Project project) {
        final McpMinecraftExtension mcpMinecraftExtension = project.getExtensions().getByType(McpMinecraftExtension.class);
        mcpMinecraftExtension.getNamingChannelProviders().register("official", namingChannelProvider -> namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::build));
        mcpMinecraftExtension.getMappings().getMappingChannel().convention(mcpMinecraftExtension.getNamingChannelProviders().named("official"));
    }

    private @NotNull TaskProvider<? extends IMcpRuntimeTask> build(@NotNull final RenamingTaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.mappingVersionData());

        final String applyTaskName = McpRuntimeUtils.buildTaskName(context.spec(), "applyOfficialMappings");
        return context.spec().project().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("mcp");
            applyOfficialMappingsToSourceJar.setDescription("Applies the Official mappings for version %s to the %s task".formatted(mappingVersion, context.taskOutputToModify().getName()));

            applyOfficialMappingsToSourceJar.getClientMappings().set(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappings().set(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getTsrgMappings().set(new File(context.unpackedMcpZipDirectory(), Objects.requireNonNull(context.mcpConfig().getData(McpConfigConstants.Data.MAPPINGS))));

            applyOfficialMappingsToSourceJar.getInput().set(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }
}
