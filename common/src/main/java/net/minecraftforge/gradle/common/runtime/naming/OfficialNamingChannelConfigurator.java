package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToSourceJar;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.util.MappingUtils;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import net.minecraftforge.gradle.mcp.extensions.MappingsExtension;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import net.minecraftforge.gradle.mcp.util.McpConfigConstants;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OfficialNamingChannelConfigurator {
    private static final OfficialNamingChannelConfigurator INSTANCE = new OfficialNamingChannelConfigurator();

    public static OfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    private OfficialNamingChannelConfigurator() {
    }

    public void configure(final Project project) {
        final McpMinecraftExtension mcpMinecraftExtension = project.getExtensions().getByType(McpMinecraftExtension.class);

        final MappingsExtension mappingsExtension = mcpMinecraftExtension.getMappings();
        mappingsExtension.getExtensions().add(TypeOf.typeOf(Boolean.class), "acceptMojangEula", false);

        mcpMinecraftExtension.getNamingChannelProviders().register("official", namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::build);
            namingChannelProvider.getHasAcceptedLicense().convention(project.provider(() -> (Boolean) mappingsExtension.getExtensions().getByName("acceptMojangEula")));
            namingChannelProvider.getLicenseText().set(getLicenseText(project));
        });
        mcpMinecraftExtension.getMappings().getMappingChannel().convention(mcpMinecraftExtension.getNamingChannelProviders().named("official"));

    }

    private @NotNull TaskProvider<? extends IMcpRuntimeTask> build(@NotNull final RenamingTaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.mappingVersionData());

        final String applyTaskName = McpRuntimeUtils.buildTaskName(context.spec(), "applyOfficialMappings");
        return context.spec().project().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("mcp");
            applyOfficialMappingsToSourceJar.setDescription(String.format("Applies the Official mappings for version %s to the %s task", mappingVersion, context.taskOutputToModify().getName()));

            applyOfficialMappingsToSourceJar.getClientMappings().set(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappings().set(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getTsrgMappings().set(new File(context.unpackedMcpZipDirectory(), Objects.requireNonNull(context.mcpConfig().getData(McpConfigConstants.Data.MAPPINGS))));

            applyOfficialMappingsToSourceJar.getInput().set(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }

    private @NotNull Provider<String> getLicenseText(Project project) {
        final MinecraftArtifactCacheExtension cacheExtension = project.getExtensions().getByType(MinecraftArtifactCacheExtension.class);
        final McpRuntimeExtension mcpRuntimeExtension = project.getExtensions().getByType(McpRuntimeExtension.class);
        return mcpRuntimeExtension.getRuntimes()
                .map(runtimes -> runtimes.values().stream().map(runtime -> runtime.spec().minecraftVersion()).distinct().collect(Collectors.toList()))
                .map((Transformer<List<File>, List<String>>) minecraftVersions -> {
                    if (minecraftVersions.isEmpty()) {
                        return Collections.emptyList();
                    }

                    return minecraftVersions.stream().map(version -> cacheExtension.cacheVersionMappings(version, ArtifactSide.CLIENT)).collect(Collectors.toList());
                })
                .map((Transformer<List<String>, List<File>>) mappingFiles -> {
                    if (mappingFiles.isEmpty())
                        return Collections.emptyList();

                    return mappingFiles.stream().map(mappingFile -> {
                        try(final Stream<String> lines = Files.lines(mappingFile.toPath())) {
                            return lines
                                    .filter(line -> line.startsWith("#"))
                                    .map(l -> l.substring(1).trim())
                                    .collect(Collectors.joining("\n"));
                        } catch (IOException e) {
                            throw new RuntimeException(String.format("Failed to read the mapping license from: %s", mappingFile.getAbsolutePath()), e);
                        }
                    }).distinct().collect(Collectors.toList());
                })
                .map(licenses -> {
                    if (licenses.isEmpty()) {
                        return "No license text found";
                    }

                    return String.join("\n\n", licenses);
                });

    }
}
