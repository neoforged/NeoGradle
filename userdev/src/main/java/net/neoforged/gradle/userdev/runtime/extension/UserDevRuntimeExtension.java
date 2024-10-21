package net.neoforged.gradle.userdev.runtime.extension;

import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.SourceAccessTransformer;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.run.TypesUtil;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Conventions;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.InjectZipContent;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.neoform.util.NeoFormAccessTaskAdapterUtils;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class UserDevRuntimeExtension extends CommonRuntimeExtension<UserDevRuntimeSpecification, UserDevRuntimeSpecification.Builder, UserDevRuntimeDefinition> {
    
    @javax.inject.Inject
    public UserDevRuntimeExtension(Project project) {
        super(project);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull UserDevRuntimeDefinition doCreate(UserDevRuntimeSpecification spec) {
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);

        final UserdevProfile userDevProfile = spec.getProfile();
        final FileTree userDevJar = spec.getUserDevArchive();

        final Configuration userDevAdditionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(
                getProject(),
                "AdditionalDependenciesFor" + spec.getIdentifier()
        );
        for (String dependencyCoordinate : userDevProfile.getAdditionalDependencyArtifactCoordinates().get()) {
            userDevAdditionalDependenciesConfiguration.getDependencies().add(getProject().getDependencies().create(dependencyCoordinate));
        }
        
        if (!userDevProfile.getNeoForm().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has no MCP version. As of now this is not supported!");
        }

        final NeoFormRuntimeDefinition neoFormRuntimeDefinition = neoFormRuntimeExtension.maybeCreate(builder -> {
            builder.withNeoFormDependency(userDevProfile.getNeoForm().get())
                    .withDistributionType(DistributionType.JOINED)
                    .withAdditionalDependencies(getProject().files(userDevAdditionalDependenciesConfiguration));

            final TaskTreeAdapter atAndIISAdapter = createAccessTransformerAdapter(userDevProfile.getAccessTransformerDirectory().get(), userDevJar)
                                                            .andThen(NeoFormAccessTaskAdapterUtils.createAccessTransformerAdapter(getProject()));
            
            builder.withPostTaskAdapter("decompile", atAndIISAdapter);

            builder.withPreTaskAdapter("recompile", NeoFormAccessTaskAdapterUtils.createInterfaceInjectionAdapter(getProject()));

            builder.withPostTaskAdapter("patch", createPatchAdapter(userDevJar, userDevProfile.getSourcePatchesDirectory().get()));

            builder.withTaskCustomizer("inject", InjectZipContent.class, task -> {
                FileTree injectionDirectoryTree;
                if (userDevProfile.getInjectedFilesDirectory().isPresent()) {
                    injectionDirectoryTree = getProject().fileTree(new File(userDevProfile.getInjectedFilesDirectory().get()));
                } else {
                    injectionDirectoryTree = null;
                }

                configureNeoforgeInjects(
                        task,
                        injectionDirectoryTree,
                        ConfigurationUtils.getArtifactProvider(getProject(), "NeoForgeSourceLookupFor" + spec.getIdentifier(), userDevProfile.getSourcesJarArtifactCoordinate()),
                        ConfigurationUtils.getArtifactProvider(getProject(), "NeoForgeRawLookupFor" + spec.getIdentifier(), userDevProfile.getUniversalJarArtifactCoordinate())
                );
            });
        });

        final RunTypeManager runTypes = getProject().getExtensions().getByType(RunTypeManager.class);
        userDevProfile.getRunTypes().forEach((type) -> {
            TypesUtil.registerWithPotentialPrefix(runTypes, spec.getIdentifier(), type.getName(), type::copyTo);
        });

        final Conventions conventions = getProject().getExtensions().getByType(Subsystems.class).getConventions();
        if (conventions.getIsEnabled().get()
                && conventions.getRuns().getIsEnabled().get()
                && conventions.getRuns().getShouldDefaultRunsBeCreated().get()) {
            final RunManager runs = getProject().getExtensions().getByType(RunManager.class);
            userDevProfile.getRunTypes().forEach(runType -> {
                if (runs.getNames().contains(runType.getName())) {
                    return;
                }

                try {
                    final Run run = runs.create(runType.getName());
                    run.configure(runType);
                    run.getConfigureFromTypeWithName().set(false);
                    run.getConfigureFromDependencies().set(false);
                } catch (IllegalStateException ignored) {
                    //thrown when the dependency is added lazily. This is fine.
                }

            });
        }
        
        spec.setMinecraftVersion(neoFormRuntimeDefinition.getSpecification().getMinecraftVersion());

        return new UserDevRuntimeDefinition(
                spec,
                neoFormRuntimeDefinition,
                userDevJar,
                userDevProfile,
                userDevAdditionalDependenciesConfiguration
        );
    }

    @Override
    protected UserDevRuntimeSpecification.Builder createBuilder() {
        return UserDevRuntimeSpecification.Builder.from(getProject());
    }

    private TaskTreeAdapter createAccessTransformerAdapter(final String accessTransformerDirectory, final FileTree userDev) {
        final FileTree accessTransformerFiles =
                userDev.matching(filter -> filter.include(accessTransformerDirectory + "/**"));

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            if (accessTransformerFiles.isEmpty()) {
                // No access transformers found, so we don't need to do anything
                return null;
            }

            final TaskProvider<? extends SourceAccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createSourceAccessTransformer(definition, "Forges", accessTransformerFiles, definition.getListLibrariesTaskProvider(), definition.getAllDependencies());
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
