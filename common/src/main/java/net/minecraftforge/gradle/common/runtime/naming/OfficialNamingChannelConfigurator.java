package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToSourceJar;
import net.minecraftforge.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToAccessTransformer;
import net.minecraftforge.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToCompiledJar;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GradleInternalUtils;
import net.minecraftforge.gradle.common.util.MappingUtils;
import net.minecraftforge.gradle.common.util.NamingConstants;
import net.minecraftforge.gradle.common.util.TaskDependencyUtils;
import net.minecraftforge.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OfficialNamingChannelConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficialNamingChannelConfigurator.class);
    private static final OfficialNamingChannelConfigurator INSTANCE = new OfficialNamingChannelConfigurator();

    public static OfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    private OfficialNamingChannelConfigurator() {
    }

    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);

        final Mappings mappingsExtension = minecraftExtension.getMappings();
        mappingsExtension.getExtensions().add(TypeOf.typeOf(Boolean.class), "acceptMojangEula", false);

        minecraftExtension.getNamingChannelProviders().register("official", namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::buildApplySourceMappingTask);
            namingChannelProvider.getApplyCompiledMappingsTaskBuilder().set(this::buildApplyCompiledMappingsTask);
            namingChannelProvider.getUnapplyCompiledMappingsTaskBuilder().set(this::buildUnapplyCompiledMappingsTask);
            namingChannelProvider.getUnapplyAccessTransformerMappingsTaskBuilder().set(this::buildUnapplyAccessTransformerMappingsTask);
            namingChannelProvider.getHasAcceptedLicense().convention(project.provider(() -> (Boolean) mappingsExtension.getExtensions().getByName("acceptMojangEula")));
            namingChannelProvider.getLicenseText().set(getLicenseText(project));
        });
        minecraftExtension.getMappings().getChannel().convention(minecraftExtension.getNamingChannelProviders().named("official"));
    }

    private TaskProvider<? extends Runtime> buildUnapplyAccessTransformerMappingsTask(TaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.getMappingVersion());

        final String applyTaskName = context.getTaskNameBuilder().apply("unApplyAccessTransformer");
        return context.getProject().getTasks().register(applyTaskName, UnapplyOfficialMappingsToAccessTransformer.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription(String.format("Unapplies the Official mappings for version %s.", mappingVersion));

            task.getClientMappings().set(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(WithOutput::getOutput));
            task.getServerMappings().set(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(WithOutput::getOutput));

            task.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
        });
    }

    private @NotNull TaskProvider<? extends Runtime> buildApplySourceMappingTask(@NotNull final TaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.getMappingVersion());

        final String applyTaskName = context.getTaskNameBuilder().apply("applyOfficialMappings");
        return context.getProject().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("mappings/official");
            applyOfficialMappingsToSourceJar.setDescription(String.format("Applies the Official mappings for version %s.", mappingVersion));

            applyOfficialMappingsToSourceJar.getClientMappings().set(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(WithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappings().set(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(WithOutput::getOutput));

            applyOfficialMappingsToSourceJar.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }

    private @NotNull TaskProvider<? extends WithOutput> buildUnapplyCompiledMappingsTask(@NotNull final TaskBuildingContext context) {
        final String unapplyTaskName = CommonRuntimeUtils.buildTaskName(context.getInputTask(), "obfuscate");

        final TaskProvider<UnapplyOfficialMappingsToCompiledJar> unapplyTask = context.getProject().getTasks().register(unapplyTaskName, UnapplyOfficialMappingsToCompiledJar.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription("Unapplies the Official mappings and re-obfuscates a compiled jar");

            if (context.getMappingVersion().containsKey(NamingConstants.Version.VERSION) || context.getMappingVersion().containsKey(NamingConstants.Version.MINECRAFT_VERSION)) {
                task.getMinecraftVersion().convention(context.getProject().provider(() -> CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(context.getMappingVersion()))));
            } else {
                task.getMinecraftVersion().convention(context.getInputTask().map(t -> {
                    try {
                        return CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(TaskDependencyUtils.extractRuntimeDefinition(context.getProject(), t).configuredMappingVersionData()));
                    } catch (MultipleDefinitionsFoundException e) {
                        throw new RuntimeException("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.spec().getName()).collect(Collectors.joining(", ")), e);
                    }
                }));
            }

            task.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
            task.getOutput().set(context.getProject().getLayout().getBuildDirectory().dir("obfuscation/" + context.getInputTask().getName()).flatMap(directory -> directory.file(context.getInputTask().flatMap(WithOutput::getOutputFileName))));
        });

        context.getInputTask().configure(task -> task.finalizedBy(unapplyTask));

        return unapplyTask;
    }

    private @NotNull TaskProvider<? extends WithOutput> buildApplyCompiledMappingsTask(@NotNull final TaskBuildingContext context) {
        final String ApplyTaskName = CommonRuntimeUtils.buildTaskName(context.getInputTask(), "deobfuscate");

        final TaskProvider<ApplyOfficialMappingsToCompiledJar> ApplyTask = context.getProject().getTasks().register(ApplyTaskName, ApplyOfficialMappingsToCompiledJar.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription("Unapplies the Official mappings and re-obfuscates a compiled jar");

            if (context.getMappingVersion().containsKey(NamingConstants.Version.VERSION) || context.getMappingVersion().containsKey(NamingConstants.Version.MINECRAFT_VERSION)) {
                task.getMinecraftVersion().convention(context.getProject().provider(() -> CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(context.getMappingVersion()))));
            } else {
                task.getMinecraftVersion().convention(context.getInputTask().map(t -> {
                    try {
                        return CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(TaskDependencyUtils.extractRuntimeDefinition(context.getProject(), t).configuredMappingVersionData()));
                    } catch (MultipleDefinitionsFoundException e) {
                        throw new RuntimeException("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.spec().getName()).collect(Collectors.joining(", ")), e);
                    }
                }));
            }

            task.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
            task.getOutput().set(context.getProject().getLayout().getBuildDirectory().dir("obfuscation/" + context.getInputTask().getName()).flatMap(directory -> directory.file(context.getInputTask().flatMap(WithOutput::getOutputFileName))));
        });

        context.getInputTask().configure(task -> task.finalizedBy(ApplyTask));

        return ApplyTask;
    }

    private @NotNull Provider<String> getLicenseText(Project project) {
        final MinecraftArtifactCache cacheExtension = project.getExtensions().getByType(MinecraftArtifactCache.class);

        return project.provider(() -> GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .collect(Collectors.toList()))
                .map(runtimeExtensions -> runtimeExtensions.stream().map(runtimeExtension -> runtimeExtension.getRuntimes()
                        .map(runtimes -> runtimes.values().stream().map(runtime -> runtime.spec().getMinecraftVersion()).distinct().collect(Collectors.toList()))
                        .map((Transformer<List<File>, List<String>>) minecraftVersions -> {
                            if (minecraftVersions.isEmpty()) {
                                return Collections.emptyList();
                            }

                            return minecraftVersions.stream().map(version -> cacheExtension.cacheVersionMappings(version, DistributionType.CLIENT)).collect(Collectors.toList());
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

