package net.neoforged.gradle.userdev.runtime.extension;

import com.google.gson.Gson;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.AccessTransformer;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.run.TypesUtil;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.*;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.Download;
import net.neoforged.gradle.neoform.runtime.tasks.InjectCode;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.neoform.runtime.tasks.UnpackZip;
import net.neoforged.gradle.neoform.util.NeoFormAccessTransformerUtils;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public abstract class UserDevRuntimeExtension extends CommonRuntimeExtension<UserDevRuntimeSpecification, UserDevRuntimeSpecification.Builder, UserDevRuntimeDefinition> {
    
    @Inject
    public UserDevRuntimeExtension(Project project) {
        super(project);
    }
    
    @Override
    protected @NotNull UserDevRuntimeDefinition doCreate(UserDevRuntimeSpecification spec) {
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);
        
        final Dependency userDevDependency = getProject().getDependencies().create(String.format("%s:%s:%s:userdev", spec.getForgeGroup(), spec.getForgeName(), spec.getForgeVersion()));
        final Configuration userDevConfiguration = ConfigurationUtils.temporaryConfiguration(getProject(), userDevDependency);
        final ResolvedConfiguration resolvedUserDevConfiguration = userDevConfiguration.getResolvedConfiguration();
        final File userDevJar = resolvedUserDevConfiguration.getFiles().iterator().next();
        
        final File forgeDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("neoForge/%s", spec.getIdentifier())).get().getAsFile();
        final File unpackedForgeDirectory = new File(forgeDirectory, "unpacked");
        
        unpackedForgeDirectory.mkdirs();
        
        final FileTree userDevJarZipTree = spec.getProject().zipTree(userDevJar);
        final CopyingFileTreeVisitor unpackingVisitor = new CopyingFileTreeVisitor(unpackedForgeDirectory);
        userDevJarZipTree.visit(unpackingVisitor);
        
        final File userDevConfigFile = new File(unpackedForgeDirectory, "config.json");
        final Gson userdevGson = UserdevProfile.createGson(getProject().getObjects());
        final UserdevProfile userDevConfigurationSpec;
        try(final FileInputStream fileInputStream = new FileInputStream(userDevConfigFile)) {
            userDevConfigurationSpec = userdevGson.fromJson(new InputStreamReader(fileInputStream), UserdevProfile.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final Configuration userDevAdditionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(getProject());
        for (String dependencyCoordinate : userDevConfigurationSpec.getAdditionalDependencyArtifactCoordinates().get()) {
            userDevAdditionalDependenciesConfiguration.getDependencies().add(getProject().getDependencies().create(dependencyCoordinate));
        }
        
        if (!userDevConfigurationSpec.getNeoForm().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has no MCP version. As of now this is not supported!");
        }
        
        final Artifact neoFormArtifact = Artifact.from(userDevConfigurationSpec.getNeoForm().get());
        
        final NeoFormRuntimeDefinition mcpRuntimeDefinition = neoFormRuntimeExtension.maybeCreate(builder -> {
            builder.withNeoFormArtifact(neoFormArtifact)
                    .withDistributionType(DistributionType.JOINED)
                    .withAdditionalDependencies(getProject().files(userDevAdditionalDependenciesConfiguration));
            
            final TaskTreeAdapter atAndSASAdapter = createAccessTransformerAdapter(userDevConfigurationSpec.getAccessTransformerDirectory().get(), unpackedForgeDirectory, getProject())
                                                            .andThen(NeoFormAccessTransformerUtils.createAccessTransformerAdapter(getProject()));
            
            builder.withPreTaskAdapter("decompile", atAndSASAdapter);
            
            final TaskTreeAdapter patchAdapter = createPatchAdapter(userDevConfigurationSpec.getSourcePatchesDirectory().get(), unpackedForgeDirectory);
            final Provider<TaskTreeAdapter> optionalInjectionAdapter = createInjectionAdapter(userDevConfigurationSpec.getInjectedFilesDirectory(), unpackedForgeDirectory);
            
            final TaskTreeAdapter resultingAdapter = optionalInjectionAdapter.map(inject -> inject.andThen(patchAdapter)).getOrElse(patchAdapter);
            final TaskTreeAdapter withForgeSourcesAdapter = userDevConfigurationSpec.getSourcesJarArtifactCoordinate().map(sources -> resultingAdapter.andThen(createInjectForgeSourcesAdapter(sources))).getOrElse(resultingAdapter);
            final TaskTreeAdapter withForgeResourcesAdapter = userDevConfigurationSpec.getUniversalJarArtifactCoordinate().map(resources -> withForgeSourcesAdapter.andThen(createInjectResourcesAdapter(resources))).getOrElse(withForgeSourcesAdapter);
            
            builder.withPostTaskAdapter("patch", withForgeResourcesAdapter);
        });
        
        spec.setMinecraftVersion(mcpRuntimeDefinition.getSpecification().getMinecraftVersion());
        
        getProject().getExtensions().configure(RunsConstants.Extensions.RUN_TYPES, (Action<NamedDomainObjectContainer<RunType>>) types -> userDevConfigurationSpec.getRunTypes().forEach((type) -> {
            TypesUtil.registerWithPotentialPrefix(types, spec.getIdentifier(), type.getName(), type::copyTo);
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
    
    private TaskTreeAdapter createAccessTransformerAdapter(final String accessTransformerDirectory, final File unpackedForgeUserDevDirectory, final Project project) {
        final Set<File> accessTransformerFiles = project.fileTree(new File(unpackedForgeUserDevDirectory, accessTransformerDirectory)).getFiles();
        
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends AccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createAccessTransformer(definition, "Forges", runtimeWorkspace, dependentTaskConfigurationHandler, accessTransformerFiles, Collections.emptyList());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }
    
    private Provider<TaskTreeAdapter> createInjectionAdapter(final Provider<String> injectionDirectory, final File unpackedForgeUserDevDirectory) {
        return injectionDirectory.map(s -> {
            File directory = new File(unpackedForgeUserDevDirectory, s);
            if (!directory.exists()) return null;
            return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) ->
                    definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "injectUserDev"), InjectCode.class, task -> {
                        task.getInjectionSource().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                        task.getInjectionDirectory().fileValue(directory);
                    });
        });
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
