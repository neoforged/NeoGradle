package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToSourceJar;
import net.minecraftforge.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToCompiledJar;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.*;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
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
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::buildApplySourceMappingTask);
            namingChannelProvider.getApplyCompiledMappingsTaskBuilder().set(this::buildApplyCompiledMappingsTask);
            namingChannelProvider.getUnapplyCompiledMappingsTaskBuilder().set(this::buildUnapplyCompiledMappingsTask);
            namingChannelProvider.getHasAcceptedLicense().convention(project.provider(() -> (Boolean) mappingsExtension.getExtensions().getByName("acceptMojangEula")));
            namingChannelProvider.getLicenseText().set(getLicenseText(project));
        });
        minecraftExtension.getMappings().getMappingChannel().convention(minecraftExtension.getNamingChannelProviders().named("official"));
    }

    private @NotNull TaskProvider<? extends IRuntimeTask> buildApplySourceMappingTask(@NotNull final ApplyMappingsTaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.mappingVersionData());

        final String applyTaskName = CommonRuntimeUtils.buildTaskName(context.environmentName(), "applyOfficialMappings");
        return context.project().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("mappings/official");
            applyOfficialMappingsToSourceJar.setDescription(String.format("Applies the Official mappings for version %s.", mappingVersion));

            applyOfficialMappingsToSourceJar.getClientMappings().set(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(ITaskWithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappings().set(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(ITaskWithOutput::getOutput));

            applyOfficialMappingsToSourceJar.getInput().set(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.gameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }

    private @NotNull TaskProvider<? extends ITaskWithOutput> buildUnapplyCompiledMappingsTask(@NotNull final UnapplyMappingsTaskBuildingContext context) {
        final String unapplyTaskName = CommonRuntimeUtils.buildTaskName(context.taskOutputToModify(), "obfuscate");

        final TaskProvider<UnapplyOfficialMappingsToCompiledJar> unapplyTask = context.project().getTasks().register(unapplyTaskName, UnapplyOfficialMappingsToCompiledJar.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription("Unapplies the Official mappings and re-obfuscates a compiled jar");

            task.getMinecraftVersion().convention(context.project().provider(() -> {
                if (context.mappingVersionData().containsKey(NamingConstants.Version.VERSION) || context.mappingVersionData().containsKey(NamingConstants.Version.MINECRAFT_VERSION)) {
                    return CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(context.mappingVersionData()));
                } else {

                    //This means we need to walk the tree -> this is a bad idea, but it's the only way to do it.
                    return context.taskOutputToModify().get().getTaskDependencies().getDependencies(task).stream().filter(JavaCompile.class::isInstance).map(JavaCompile.class::cast)
                            .findFirst()
                            .map(JavaCompile::getClasspath)
                            .map(classpath -> classpath.getBuildDependencies().getDependencies(null))
                            .flatMap(depedendencies -> depedendencies.stream().filter(ArtifactFromOutput.class::isInstance).map(ArtifactFromOutput.class::cast).findFirst())
                            .flatMap(artifactTask -> artifactTask.getTaskDependencies().getDependencies(artifactTask).stream().filter(ArtifactProvider.class::isInstance).map(ArtifactProvider.class::cast).findFirst())
                            .map(artifactProvider -> {
                                final CommonRuntimeExtension<?,?,? extends CommonRuntimeDefinition<?>> runtimeExtension = context.project().getExtensions().getByType(CommonRuntimeExtension.class);
                                return runtimeExtension.getRuntimes().get()
                                        .values()
                                        .stream()
                                        .filter(runtime -> runtime.rawJarTask().get().equals(artifactProvider))
                                        .map(runtime -> runtime.spec().minecraftVersion())
                                        .map(CacheableMinecraftVersion::from)
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("Could not find minecraft version for task " + context.taskOutputToModify().getName()));
                            })
                            .orElseThrow(() -> new IllegalStateException("Could not find minecraft version for task " + context.taskOutputToModify().getName()));
                }
            }));
            task.getInput().set(context.taskOutputToModify().flatMap(Jar::getArchiveFile));
            task.getOutput().set(context.project().getLayout().getBuildDirectory().dir("obfuscation/" + context.taskOutputToModify().getName()).flatMap(directory -> directory.file(context.taskOutputToModify().flatMap(AbstractArchiveTask::getArchiveFileName))));
        });

        context.taskOutputToModify().configure(task -> task.finalizedBy(unapplyTask));

        return unapplyTask;
    }

    private @NotNull TaskProvider<? extends ITaskWithOutput> buildApplyCompiledMappingsTask(@NotNull final ApplyMappingsTaskBuildingContext context) {
        final String ApplyTaskName = CommonRuntimeUtils.buildTaskName(context.taskOutputToModify(), "deobfuscate");

        final TaskProvider<ApplyOfficialMappingsToCompiledJar> ApplyTask = context.project().getTasks().register(ApplyTaskName, ApplyOfficialMappingsToCompiledJar.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription("Unapplies the Official mappings and re-obfuscates a compiled jar");

            task.getMinecraftVersion().convention(context.project().provider(() -> {
                if (context.mappingVersionData().containsKey(NamingConstants.Version.VERSION) || context.mappingVersionData().containsKey(NamingConstants.Version.MINECRAFT_VERSION)) {
                    return CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(context.mappingVersionData()));
                } else {

                    //This means we need to walk the tree -> this is a bad idea, but it's the only way to do it.
                    return context.taskOutputToModify().get().getTaskDependencies().getDependencies(task).stream().filter(JavaCompile.class::isInstance).map(JavaCompile.class::cast)
                            .findFirst()
                            .map(JavaCompile::getClasspath)
                            .map(classpath -> classpath.getBuildDependencies().getDependencies(null))
                            .flatMap(depedendencies -> depedendencies.stream().filter(ArtifactFromOutput.class::isInstance).map(ArtifactFromOutput.class::cast).findFirst())
                            .flatMap(artifactTask -> artifactTask.getTaskDependencies().getDependencies(artifactTask).stream().filter(ArtifactProvider.class::isInstance).map(ArtifactProvider.class::cast).findFirst())
                            .map(artifactProvider -> {
                                final CommonRuntimeExtension<?,?,? extends CommonRuntimeDefinition<?>> runtimeExtension = context.project().getExtensions().getByType(CommonRuntimeExtension.class);
                                return runtimeExtension.getRuntimes().get()
                                        .values()
                                        .stream()
                                        .filter(runtime -> runtime.rawJarTask().get().equals(artifactProvider))
                                        .map(runtime -> runtime.spec().minecraftVersion())
                                        .map(CacheableMinecraftVersion::from)
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("Could not find minecraft version for task " + context.taskOutputToModify().getName()));
                            })
                            .orElseThrow(() -> new IllegalStateException("Could not find minecraft version for task " + context.taskOutputToModify().getName()));
                }
            }));
            task.getInput().set(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutput));
            task.getOutput().set(context.project().getLayout().getBuildDirectory().dir("obfuscation/" + context.taskOutputToModify().getName()).flatMap(directory -> directory.file(context.taskOutputToModify().flatMap(ITaskWithOutput::getOutputFileName))));
        });

        context.taskOutputToModify().configure(task -> task.finalizedBy(ApplyTask));

        return ApplyTask;
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

