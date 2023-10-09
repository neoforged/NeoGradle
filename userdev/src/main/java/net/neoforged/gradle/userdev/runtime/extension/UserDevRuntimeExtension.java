package net.neoforged.gradle.userdev.runtime.extension;

import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.AccessTransformer;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.run.TypesUtil;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.*;
import net.neoforged.gradle.neoform.util.NeoFormAccessTransformerUtils;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import net.neoforged.gradle.userdev.utils.UserDevConfigurationSpecUtils;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class UserDevRuntimeExtension extends CommonRuntimeExtension<UserDevRuntimeSpecification, UserDevRuntimeSpecification.Builder, UserDevRuntimeDefinition> {
    
    @Inject
    public UserDevRuntimeExtension(Project project) {
        super(project);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull UserDevRuntimeDefinition doCreate(UserDevRuntimeSpecification spec) {
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);
        
        final Dependency userDevDependency = getProject().getDependencies().create(String.format("%s:%s:%s:userdev", spec.getForgeGroup(), spec.getForgeName(), spec.getForgeVersion()));
        final Configuration userDevConfiguration = ConfigurationUtils.temporaryConfiguration(getProject(), userDevDependency);
        final ResolvedConfiguration resolvedUserDevConfiguration = userDevConfiguration.getResolvedConfiguration();
        final File userDevJar = resolvedUserDevConfiguration.getFiles().iterator().next();
        
        final File forgeDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("forge/%s", spec.getIdentifier())).get().getAsFile();
        final File unpackedForgeDirectory = new File(forgeDirectory, "unpacked");
        
        unpackedForgeDirectory.mkdirs();
        
        final FileTree userDevJarZipTree = spec.getProject().zipTree(userDevJar);
        final CopyingFileTreeVisitor unpackingVisitor = new CopyingFileTreeVisitor(unpackedForgeDirectory);
        userDevJarZipTree.visit(unpackingVisitor);
        
        final File userDevConfigFile = new File(unpackedForgeDirectory, "config.json");
        final UserDevConfigurationSpecV2 userDevConfigurationSpec = UserDevConfigurationSpecUtils.get(getProject(), userDevConfigFile);
        
        final Configuration userDevAdditionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(getProject());
        for (String dependencyCoordinate : userDevConfigurationSpec.getAdditionalDependencies()) {
            userDevAdditionalDependenciesConfiguration.getDependencies().add(getProject().getDependencies().create(dependencyCoordinate));
        }
        
        if (userDevConfigurationSpec.getParentName().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has parent name. As of now this is not supported!");
        }
        
        if (!userDevConfigurationSpec.getMcpVersion().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has no MCP version. As of now this is not supported!");
        }
        
        final Artifact neoFormArtifact = Artifact.from(userDevConfigurationSpec.getMcpVersion().get());
        
        final NeoFormRuntimeDefinition mcpRuntimeDefinition = neoFormRuntimeExtension.maybeCreate(builder -> {
            builder.withNeoFormArtifact(neoFormArtifact)
                    .withDistributionType(DistributionType.JOINED)
                    .withAdditionalDependencies(getProject().files(userDevAdditionalDependenciesConfiguration));
            
            final TaskTreeAdapter atAndSASAdapter = createAccessTransformerAdapter(userDevConfigurationSpec.getAccessTransformerPaths(), unpackedForgeDirectory)
                                                            .andThen(createSideAnnotationStripperAdapter(userDevConfigurationSpec.getSideAnnotationStripperPaths(), unpackedForgeDirectory))
                                                            .andThen(NeoFormAccessTransformerUtils.createAccessTransformerAdapter(getProject()));
            
            builder.withPreTaskAdapter("decompile", atAndSASAdapter);
            
            final TaskTreeAdapter patchAdapter = createPatchAdapter(userDevConfigurationSpec.getSourcePatchesDirectory(), unpackedForgeDirectory);
            final Optional<TaskTreeAdapter> optionalInjectionAdapter = createInjectionAdapter(userDevConfigurationSpec.getInjectedFilesDirectory(), unpackedForgeDirectory);
            
            final TaskTreeAdapter resultingAdapter = optionalInjectionAdapter.map(inject -> inject.andThen(patchAdapter)).orElse(patchAdapter);
            final TaskTreeAdapter withForgeSourcesAdapter = userDevConfigurationSpec.getSourcesArtifactIdentifier().map(sources -> resultingAdapter.andThen(createInjectForgeSourcesAdapter(sources))).orElse(resultingAdapter);
            final TaskTreeAdapter withForgeResourcesAdapter = userDevConfigurationSpec.getUniversalArtifactIdentifier().map(resources -> withForgeSourcesAdapter.andThen(createInjectResourcesAdapter(resources))).orElse(withForgeSourcesAdapter);
            
            builder.withPostTaskAdapter("patch", withForgeResourcesAdapter);
        });
        
        spec.setMinecraftVersion(mcpRuntimeDefinition.getSpecification().getMinecraftVersion());
        
        getProject().getExtensions().configure(RunsConstants.Extensions.RUN_TYPES, (Action<NamedDomainObjectContainer<RunType>>) types -> userDevConfigurationSpec.getRunTypes().forEach((name, type) -> {
            TypesUtil.registerWithPotentialPrefix(types, spec.getIdentifier(), name, type::copyTo);
        }));
        
        return new UserDevRuntimeDefinition(
                spec,
                mcpRuntimeDefinition,
                unpackedForgeDirectory,
                userDevConfigurationSpec,
                userDevAdditionalDependenciesConfiguration
        );
    }
    
    @Override
    protected UserDevRuntimeSpecification.Builder createBuilder() {
        return UserDevRuntimeSpecification.Builder.from(getProject());
    }
    
    @Override
    protected void bakeDefinition(UserDevRuntimeDefinition definition) {
        final UserDevRuntimeSpecification spec = definition.getSpecification();
        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        
        definition.onBake(
                mappingsExtension.getChannel().get(),
                spec.getProject().getLayout().getBuildDirectory().get().dir("userdev").dir(spec.getIdentifier()).getAsFile()
        );
    }
    
    private TaskTreeAdapter createAccessTransformerAdapter(final List<String> accessTransformerPaths, final File unpackedForgeUserDevDirectory) {
        final List<File> accessTransformerFiles = accessTransformerPaths.stream().map(path -> new File(unpackedForgeUserDevDirectory, path)).collect(Collectors.toList());
        
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends AccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createAccessTransformer(definition, "Forges", runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler, accessTransformerFiles, Collections.emptyList());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }
    
    private TaskTreeAdapter createSideAnnotationStripperAdapter(final List<String> sideAnnotationStripperPaths, final File unpackedForgeUserDevDirectory) {
        final List<File> sideAnnotationStripperFiles = sideAnnotationStripperPaths.stream().map(path -> new File(unpackedForgeUserDevDirectory, path)).collect(Collectors.toList());
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends SideAnnotationStripper> sideAnnotationStripper = NeoFormRuntimeUtils.createSideAnnotationStripper(definition.getSpecification(), "Forges", sideAnnotationStripperFiles, Collections.emptyList());
            sideAnnotationStripper.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            sideAnnotationStripper.configure(task -> task.dependsOn(previousTasksOutput));
            return sideAnnotationStripper;
        };
    }
    
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<TaskTreeAdapter> createInjectionAdapter(final Optional<String> injectionDirectory, final File unpackedForgeUserDevDirectory) {
        return injectionDirectory.map(s -> (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "injectUserDev"), InjectCode.class, task -> {
            task.getInjectionSource().set(previousTasksOutput.flatMap(WithOutput::getOutput));
            task.getInjectionDirectory().fileValue(new File(unpackedForgeUserDevDirectory, s));
        }));
        
    }
    
    private TaskTreeAdapter createPatchAdapter(final String patchDirectory, final File unpackForgeUserDevDirectory) {
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "patchUserDev"), Patch.class, task -> {
            task.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
            task.getPatchDirectory().fileProvider(definition.getSpecification().getProject().provider(() -> new File(unpackForgeUserDevDirectory, patchDirectory)));
        });
    }
    
    private TaskTreeAdapter createInjectForgeSourcesAdapter(final String forgeSourcesCoordinate) {
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final Configuration forgeSourcesConfiguration = ConfigurationUtils.temporaryConfiguration(definition.getSpecification().getProject(), definition.getSpecification().getProject().getDependencies().create(forgeSourcesCoordinate));
            final TaskProvider<? extends Download> downloadForgeSources = definition.getSpecification().getProject().getTasks()
                                                                                  .register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "downloadForgesSources"), Download.class, task -> {
                                                                                      task.getInput().from(forgeSourcesConfiguration);
                                                                                  });
            
            dependentTaskConfigurationHandler.accept(downloadForgeSources);
            
            final TaskProvider<? extends UnpackZip> unzipForgeSources = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "unzipForgesSources"), UnpackZip.class, task -> {
                task.getInputZip().set(downloadForgeSources.flatMap(Download::getOutput));
                task.dependsOn(downloadForgeSources);
            });
            
            dependentTaskConfigurationHandler.accept(unzipForgeSources);
            
            return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "injectForgesSources"), InjectCode.class, task -> {
                task.getInjectionSource().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                task.getInjectionDirectory().set(unzipForgeSources.flatMap(UnpackZip::getUnpackingTarget));
                task.getInclusionFilter().add("net/**");
                task.dependsOn(unzipForgeSources);
            });
        };
    }
    
    private TaskTreeAdapter createInjectResourcesAdapter(final String forgeUniversalCoordinate) {
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final Configuration forgeUniversalConfiguration = ConfigurationUtils.temporaryConfiguration(definition.getSpecification().getProject(), definition.getSpecification().getProject().getDependencies().create(forgeUniversalCoordinate));
            final TaskProvider<? extends Download> downloadForgeUniversal = definition.getSpecification().getProject().getTasks()
                                                                                    .register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "downloadForgeUniversal"), Download.class, task -> {
                                                                                        task.getInput().from(forgeUniversalConfiguration);
                                                                                    });
            
            dependentTaskConfigurationHandler.accept(downloadForgeUniversal);
            
            final TaskProvider<? extends UnpackZip> unzipForgeUniversal = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "unzipForgeUniversal"), UnpackZip.class, task -> {
                task.getInputZip().set(downloadForgeUniversal.flatMap(Download::getOutput));
                task.dependsOn(downloadForgeUniversal);
            });
            
            dependentTaskConfigurationHandler.accept(unzipForgeUniversal);
            
            return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "injectForgeResources"), InjectCode.class, task -> {
                task.getInjectionSource().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                task.getInjectionDirectory().set(unzipForgeUniversal.flatMap(UnpackZip::getUnpackingTarget));
                task.getExclusionFilter().add("**/*.class");
                task.getExclusionFilter().add("META-INF/**/*.DSA");
                task.getExclusionFilter().add("**/*.SF");
                task.dependsOn(unzipForgeUniversal);
            });
        };
    }
    
}
