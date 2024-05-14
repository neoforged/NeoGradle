package net.neoforged.gradle.userdev.runtime.extension;

import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.AccessTransformer;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.run.TypesUtil;
import net.neoforged.gradle.dsl.common.extensions.ConfigurationData;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.InjectZipContent;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.neoform.util.NeoFormAccessTransformerUtils;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public abstract class UserDevRuntimeExtension extends CommonRuntimeExtension<UserDevRuntimeSpecification, UserDevRuntimeSpecification.Builder, UserDevRuntimeDefinition> {
    
    @javax.inject.Inject
    public UserDevRuntimeExtension(Project project) {
        super(project);
    }
    
    @Override
    protected @NotNull UserDevRuntimeDefinition doCreate(UserDevRuntimeSpecification spec) {
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);

        final UserdevProfile userdevProfile = spec.getProfile();
        final FileTree userDevJar = spec.getUserDevArchive();

        final Configuration userDevAdditionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(getProject());
        for (String dependencyCoordinate : userdevProfile.getAdditionalDependencyArtifactCoordinates().get()) {
            userDevAdditionalDependenciesConfiguration.getDependencies().add(getProject().getDependencies().create(dependencyCoordinate));
        }
        
        if (!userdevProfile.getNeoForm().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has no MCP version. As of now this is not supported!");
        }

        final NeoFormRuntimeDefinition mcpRuntimeDefinition = neoFormRuntimeExtension.maybeCreate(builder -> {
            builder.withNeoFormDependency(userdevProfile.getNeoForm().get())
                    .withDistributionType(DistributionType.JOINED)
                    .withAdditionalDependencies(getProject().files(userDevAdditionalDependenciesConfiguration));

            final TaskTreeAdapter atAdapter = createAccessTransformerAdapter(userdevProfile.getAccessTransformerDirectory().get(), userDevJar)
                                                            .andThen(NeoFormAccessTransformerUtils.createAccessTransformerAdapter(getProject()));
            
            builder.withPostTaskAdapter("decompile", atAdapter);

            builder.withPostTaskAdapter("patch", createPatchAdapter(userDevJar, userdevProfile.getSourcePatchesDirectory().get()));

            builder.withTaskCustomizer("inject", InjectZipContent.class, task -> {
                FileTree injectionDirectoryTree;
                if (userdevProfile.getInjectedFilesDirectory().isPresent()) {
                    injectionDirectoryTree = getProject().fileTree(new File(userdevProfile.getInjectedFilesDirectory().get()));
                } else {
                    injectionDirectoryTree = null;
                }

                configureNeoforgeInjects(
                        task,
                        injectionDirectoryTree,
                        ConfigurationUtils.getArtifactProvider(getProject(), userdevProfile.getSourcesJarArtifactCoordinate()),
                        ConfigurationUtils.getArtifactProvider(getProject(), userdevProfile.getUniversalJarArtifactCoordinate())
                );
            });
        });
        
        spec.setMinecraftVersion(mcpRuntimeDefinition.getSpecification().getMinecraftVersion());

        final NamedDomainObjectContainer<RunType> runTypes = (NamedDomainObjectContainer<RunType>) getProject().getExtensions().getByName(RunsConstants.Extensions.RUN_TYPES);
        userdevProfile.getRunTypes().forEach((type) -> {
            TypesUtil.registerWithPotentialPrefix(runTypes, spec.getIdentifier(), type.getName(), type::copyTo);
        });
        
        return new UserDevRuntimeDefinition(
                spec,
                mcpRuntimeDefinition,
                userDevJar,
                userdevProfile,
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
    
    private TaskTreeAdapter createAccessTransformerAdapter(final String accessTransformerDirectory, final FileTree userDev) {
        final FileTree accessTransformerFiles =
                userDev.matching(filter -> filter.include(accessTransformerDirectory + "/**"));

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends AccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createAccessTransformer(definition, "Forges", runtimeWorkspace, dependentTaskConfigurationHandler, accessTransformerFiles, Collections.emptyList(), definition.getListLibrariesTaskProvider());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }

    private TaskTreeAdapter createPatchAdapter(FileTree userDevArchive, String patchDirectory) {
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "patchUserDev"), Patch.class, task -> {
            task.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
            task.getPatchArchive().from(userDevArchive);
            task.getPatchDirectory().set(patchDirectory);
        });
    }

    /*
     * Configures the inject task, which runs right before patching, to also include the content that Neoforge
     * adds to the Minecraft jar, such as the Neoforge sources and resources.
     */
    private void configureNeoforgeInjects(InjectZipContent task,
                                          @Nullable FileTree userDevInjectDir,
                                          Provider<File> sourcesInjectArtifact,
                                          Provider<File> resourcesInjectArtifact) {

        if (userDevInjectDir != null) {
            task.injectFileTree(userDevInjectDir);
        }

        if (sourcesInjectArtifact.isPresent()) {
            task.injectZip(sourcesInjectArtifact, filter -> {
                filter.include("net/**");
            });
        }

        if (resourcesInjectArtifact.isPresent()) {
            task.injectZip(resourcesInjectArtifact, filter -> {
                filter.exclude("**/*.class");
                filter.exclude("META-INF/**/*.DSA");
                filter.exclude("**/*.SF");
            });
        }

    }

}
