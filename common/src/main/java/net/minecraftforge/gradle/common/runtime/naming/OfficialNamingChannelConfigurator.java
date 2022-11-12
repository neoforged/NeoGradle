package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToSourceJar;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.*;
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
        final MinecraftExtension minecraftExtension = project.getExtensions().getByType(MinecraftExtension.class);

        final MappingsExtension mappingsExtension = minecraftExtension.getMappings();
        mappingsExtension.getExtensions().add(TypeOf.typeOf(Boolean.class), "acceptMojangEula", false);

        minecraftExtension.getNamingChannelProviders().register("official", namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::build);
            namingChannelProvider.getHasAcceptedLicense().convention(project.provider(() -> (Boolean) mappingsExtension.getExtensions().getByName("acceptMojangEula")));
            namingChannelProvider.getLicenseText().set(getLicenseText(project));
        });
        minecraftExtension.getMappings().getMappingChannel().convention(minecraftExtension.getNamingChannelProviders().named("official"));

    }

    private @NotNull TaskProvider<? extends IRuntimeTask> build(@NotNull final RenamingTaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.mappingVersionData());

        final String applyTaskName = CommonRuntimeUtils.buildTaskName(context.spec(), "applyOfficialMappings");
        return context.spec().project().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("mcp");
            applyOfficialMappingsToSourceJar.setDescription(String.format("Applies the Official mappings for version %s.", mappingVersion));

            applyOfficialMappingsToSourceJar.getClientMappings().set(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappings().set(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getTsrgMappings().set(context.intermediaryMappingFile().orElse(null));

            applyOfficialMappingsToSourceJar.getInput().set(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }

    private @NotNull Provider<String> getLicenseText(Project project) {
        final MinecraftArtifactCacheExtension cacheExtension = project.getExtensions().getByType(MinecraftArtifactCacheExtension.class);

        return project.provider(() -> GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .collect(Collectors.toList()))
                .map(runtimeExtensions -> runtimeExtensions.stream().map(runtimeExtension -> runtimeExtension.getRuntimes()
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

                            return licenses.stream().distinct().collect(Collectors.joining("\n\n"));
                        })
                ).collect(Collectors.toList()))
                .map(licenses -> licenses.stream().map(Provider::get).distinct().collect(Collectors.joining("\n\n")));
    }
}
