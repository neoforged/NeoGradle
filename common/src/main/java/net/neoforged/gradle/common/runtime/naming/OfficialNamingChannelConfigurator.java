package net.neoforged.gradle.common.runtime.naming;

import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToSourceJar;
import net.neoforged.gradle.common.util.MappingUtils;
import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.runtime.extensions.RuntimesExtension;
import net.neoforged.gradle.common.runtime.naming.tasks.*;
import net.neoforged.gradle.common.util.StreamUtils;
import net.neoforged.gradle.common.util.MappingUtils;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.common.util.exceptions.NoDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.*;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
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

    @SuppressWarnings("unchecked")
    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);

        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final Property<Boolean> hasAcceptedLicenseProperty = project.getObjects().property(Boolean.class);
        mappingsExtension.getExtensions().add(TypeOf.typeOf(Property.class), "acceptMojangEula", hasAcceptedLicenseProperty);
        hasAcceptedLicenseProperty.convention(false);

        minecraftExtension.getNamingChannels().register("official", namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::buildApplySourceMappingTask);
            namingChannelProvider.getApplyCompiledMappingsTaskBuilder().set(this::buildApplyCompiledMappingsTask);
            namingChannelProvider.getHasAcceptedLicense().convention(project.provider(() -> ((Property<Boolean>) mappingsExtension.getExtensions().getByName("acceptMojangEula")).get()));
            namingChannelProvider.getLicenseText().set(getLicenseText(project));
        });
        minecraftExtension.getMappings().getChannel().convention(minecraftExtension.getNamingChannels().named("official"));
    }
    
    private @NotNull TaskProvider<? extends Runtime> buildApplySourceMappingTask(@NotNull final TaskBuildingContext context) {
        

        final String applyTaskName = context.getTaskNameBuilder().apply("applyOfficialMappings");
        return context.getProject().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            //We can realise this here, tasks are created late anyway.
            final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.getMappingVersion().get());
            
            applyOfficialMappingsToSourceJar.setGroup("mappings/official");
            applyOfficialMappingsToSourceJar.setDescription(String.format("Applies the Official mappings for version %s.", mappingVersion));

            applyOfficialMappingsToSourceJar.getClientMappingsFile().set(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(WithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappingsFile().set(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(WithOutput::getOutput));

            applyOfficialMappingsToSourceJar.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }
    
    private @NotNull TaskProvider<? extends Runtime> buildApplyCompiledMappingsTask(@NotNull final TaskBuildingContext context) {
        final String ApplyTaskName = CommonRuntimeUtils.buildTaskName(context.getInputTask(), "deobfuscate");
        
        
        final TaskProvider<ApplyOfficialMappingsToCompiledJar> applyTask = context.getProject().getTasks().register(ApplyTaskName, ApplyOfficialMappingsToCompiledJar.class, task -> {
            if (!context.getRuntimeDefinition().isPresent())
                throw new IllegalArgumentException("Cannot apply compiled mappings without a runtime definition");
            
            final TaskProvider<? extends WithOutput> librariesTask = context.getLibrariesTask();
            
            if (librariesTask == null) {
                throw new IllegalArgumentException("Cannot apply compiled mappings without a libraries task");
            }
            
            task.setGroup("mappings/official");
            task.setDescription("Applies the Official mappings and unobfuscates a compiled jar");
            
            task.getMinecraftVersion()
                    .set(context.getMappingVersion().flatMap(versionData -> {
                        if (versionData.containsKey(NamingConstants.Version.VERSION) || versionData.containsKey(NamingConstants.Version.MINECRAFT_VERSION)) {
                            return context.getProject().provider(() -> CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(versionData), context.getProject()).getFull());
                        } else {
                            return context.getInputTask().map(t -> {
                                try {
                                    return CacheableMinecraftVersion.from(MappingUtils.getVersionOrMinecraftVersion(TaskDependencyUtils.extractRuntimeDefinition(context.getProject(), t).getMappingVersionData()), context.getProject()).getFull();
                                } catch (MultipleDefinitionsFoundException e) {
                                    throw new RuntimeException("Could not determine the runtime definition to use. Multiple definitions were found: " + e.getDefinitions().stream().map(r1 -> r1.getSpecification().getVersionedName()).collect(Collectors.joining(", ")), e);
                                } catch (NoDefinitionsFoundException e) {
                                    throw new RuntimeException("Could not determine the runtime definition to use. Within the renaming context, there is none defined!", e);
                                }
                            });
                        }
                    }));
            
            task.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));
            task.getLibraries().set(librariesTask.flatMap(WithOutput::getOutput));
            task.getMappings().set(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(WithOutput::getOutput));
            
            task.dependsOn(context.getInputTask());
            task.dependsOn(librariesTask);
        });
        
        context.getInputTask().configure(task -> task.finalizedBy(applyTask));
        
        return applyTask;
    }

    private @NotNull Provider<String> getLicenseText(Project project) {
        final MinecraftArtifactCache cacheExtension = project.getExtensions().getByType(MinecraftArtifactCache.class);

        return project.provider(() -> project.getExtensions().getByType(RuntimesExtension.class).getAllDefinitions())
                .map(runtimes -> runtimes.stream().map(runtime -> runtime.getSpecification().getMinecraftVersion()).distinct().collect(Collectors.toList()))
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
                            return StreamUtils.takeWhile(lines,line -> line.startsWith("#"))
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
                });
    }
}

