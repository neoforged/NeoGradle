package net.neoforged.gradle.userdev.runtime.extension;

import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.BinaryAccessTransformer;
import net.neoforged.gradle.common.tasks.InjectInterfacesTask;
import net.neoforged.gradle.common.tasks.StripFinalFromParametersTask;
import net.neoforged.gradle.common.util.*;
import net.neoforged.gradle.common.util.run.TypesUtil;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Conventions;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Decompiler;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.InjectZipContent;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class UserDevRuntimeExtension extends CommonRuntimeExtension<UserDevRuntimeSpecification, UserDevRuntimeSpecification.Builder, UserDevRuntimeDefinition>
{

    @javax.inject.Inject
    public UserDevRuntimeExtension(Project project)
    {
        super(project);
    }

    @Override
    protected @NotNull UserDevRuntimeDefinition doCreate(UserDevRuntimeSpecification spec)
    {
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);
        final Decompiler decompilerSubsystemConfiguration = getProject().getExtensions().getByType(Subsystems.class).getDecompiler();
        final Minecraft minecraftExtension = getProject().getExtensions().getByType(Minecraft.class);

        final UserdevProfile userDevProfile = spec.getProfile();
        final FileTree userDevJar = spec.getUserDevArchive();

        final Configuration userDevAdditionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(
            getProject(),
            "AdditionalDependenciesFor" + spec.getIdentifier()
        );

        if (useCombinedJarWithNeoForgeOnRecompile(userDevProfile))
        {
            userDevAdditionalDependenciesConfiguration.getDependencies().addLater(
                userDevProfile.getUniversalJarArtifactCoordinate().map(spec.getProject().getDependencies()::create)
            );
        }

        for (String dependencyCoordinate : userDevProfile.getAdditionalDependencyArtifactCoordinates().get())
        {
            userDevAdditionalDependenciesConfiguration.getDependencies().add(getProject().getDependencies().create(dependencyCoordinate));
        }

        if (!userDevProfile.getNeoForm().isPresent())
        {
            throw new IllegalStateException("Userdev configuration spec has no MCP version. As of now this is not supported!");
        }

        final NeoFormRuntimeDefinition neoFormRuntimeDefinition = neoFormRuntimeExtension.maybeCreate(builder -> {
            builder.withNeoFormDependency(userDevProfile.getNeoForm().get())
                .withDistributionType(DistributionType.JOINED)
                .withAdditionalDependencies(getProject().files(userDevAdditionalDependenciesConfiguration))
                .withStepsMutator(this.adaptNeoFormRuntime(spec, userDevProfile, userDevJar));

            final FileTree accessTransformerFiles =
                userDevJar.matching(filter -> filter.include(userDevProfile.getAccessTransformerDirectory().get() + "/**"));

            builder.withPreTaskAdapter("recompile", JavaSourceTransformAdapterUtils.createCustomizationsAdapter(getProject(), accessTransformerFiles));

            builder.withPostTaskAdapter("patch", createPatchAdapter(userDevJar, userDevProfile.getSourcePatchesDirectory().get()));

            if (!useCombinedJarWithNeoForgeOnRecompile(userDevProfile))
            {
                builder.withTaskCustomizer("inject", InjectZipContent.class, task -> {
                    FileTree injectionDirectoryTree;
                    if (userDevProfile.getInjectedFilesDirectory().isPresent())
                    {
                        injectionDirectoryTree = getProject().fileTree(new File(userDevProfile.getInjectedFilesDirectory().get()));
                    }
                    else
                    {
                        injectionDirectoryTree = null;
                    }

                    configureNeoforgeInjects(
                        task,
                        injectionDirectoryTree,
                        ConfigurationUtils.getArtifactProvider(getProject(), "NeoForgeSourceLookupFor" + spec.getIdentifier(), userDevProfile.getSourcesJarArtifactCoordinate()),
                        ConfigurationUtils.getArtifactProvider(getProject(), "NeoForgeRawLookupFor" + spec.getIdentifier(), userDevProfile.getUniversalJarArtifactCoordinate())
                    );
                });
            } else if (decompilerSubsystemConfiguration.getIsDisabled().get()){
                builder.withPostTaskAdapter("downloadClient", new TaskTreeAdapter() {
                    @Override
                    public @NotNull TaskProvider<? extends Runtime> adapt(
                        final Definition<?> definition,
                        final Provider<? extends WithOutput> previousTasksOutput,
                        final File runtimeWorkspace,
                        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts,
                        final Map<String, String> mappingVersionData,
                        final Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler)
                    {
                        var stripper = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(),
                            "stripClientFinals"), StripFinalFromParametersTask.class, task -> {
                            task.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                        });
                        dependentTaskConfigurationHandler.accept(stripper);
                        return stripper;
                    }
                });

                builder.withPostTaskAdapter("downloadServer", new TaskTreeAdapter() {
                    @Override
                    public @NotNull TaskProvider<? extends Runtime> adapt(
                        final Definition<?> definition,
                        final Provider<? extends WithOutput> previousTasksOutput,
                        final File runtimeWorkspace,
                        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts,
                        final Map<String, String> mappingVersionData,
                        final Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler)
                    {
                        var stripper = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(),
                            "stripServerFinals"), StripFinalFromParametersTask.class, task -> {
                            task.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                        });
                        dependentTaskConfigurationHandler.accept(stripper);
                        return stripper;
                    }
                });

                builder.withPostTaskAdapter("setup", (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
                    final AccessTransformers userAts = minecraftExtension.getAccessTransformers();

                    if (userAts.getFiles().isEmpty()) {
                        return null;
                    }

                    final TaskProvider<? extends BinaryAccessTransformer> task = CommonRuntimeTaskUtils.createBinaryAccessTransformer(
                        definition,
                        "",
                        userAts.getFiles().getAsFileTree()
                    );

                    task.configure(t -> {
                        t.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                        t.dependsOn(previousTasksOutput);
                    });

                    dependentTaskConfigurationHandler.accept(task);

                    return task;
                });

                builder.withPostTaskAdapter("setup", (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
                    final InterfaceInjections userIIs = minecraftExtension.getInterfaceInjections();

                    if (userIIs.getFiles().isEmpty()) {
                        return null;
                    }

                    final TaskProvider<? extends InjectInterfacesTask> task = CommonRuntimeTaskUtils.createBinaryInterfaceInjector(
                        definition,
                        "",
                        userIIs.getFiles().getAsFileTree()
                    );

                    task.configure(t -> {
                        t.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                        t.dependsOn(previousTasksOutput);
                    });

                    dependentTaskConfigurationHandler.accept(task);

                    return task;
                });
            }
        });

        spec.setMinecraftVersion(neoFormRuntimeDefinition.getSpecification().getMinecraftVersion());

        if (!useCombinedJarWithNeoForgeOnRecompile(userDevProfile))
        {
            //Create the client-extra jar dependency.
            final Dependency clientExtraJar = spec.getProject().getDependencies().create(
                ExtraJarDependencyManager.generateClientCoordinateFor(spec.getMinecraftVersion())
            );

            //Add it as a user dev dependency, this will trigger replacement, which will need to be addressed down-below.
            userDevAdditionalDependenciesConfiguration.getDependencies().add(
                clientExtraJar
            );
        }

        return new UserDevRuntimeDefinition(
            spec,
            neoFormRuntimeDefinition,
            userDevJar,
            userDevProfile,
            userDevAdditionalDependenciesConfiguration
        );
    }

    private @NotNull BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> adaptNeoFormRuntime(
        final UserDevRuntimeSpecification spec,
        final UserdevProfile userDevProfile,
        final FileTree userDevJar)
    {
        return (steps, functions) -> {
            if (!useCombinedJarWithNeoForgeOnRecompile(userDevProfile))
            {
                return;
            }

            //We use a new PROCESS_JAR from installer tools.
            //So we yeet, merge, merge mappings, and rename
            //Additionally we need to remove the decompile, as it requires the rename to exist, we just rebuild it.
            steps.removeIf(step -> step.getType().equals("strip"));
            steps.removeIf(step -> step.getType().equals("merge"));
            steps.removeIf(step -> step.getType().equals("mergeMappings"));
            steps.removeIf(step -> step.getType().equals("rename"));

            final int decompileIndex = ListUtils.removeIfAndReturnIndex(
                steps, step -> step.getType().equals("decompile")
            );

            //Now read the decompile step, using the setup output as its input.
            steps.add(
                decompileIndex,
                new NeoFormConfigConfigurationSpecV1.Step(
                    "decompile", "decompile",
                    Map.of(
                        "libraries", "{listLibrariesOutput}",
                        "input", "{setupOutput}"
                    )
                )
            );

            final Subsystems subsystems = spec.getProject().getExtensions().getByType(Subsystems.class);
            final SetupConfiguration configuration = buildSetupConfiguration(
                userDevProfile,
                userDevJar
            );

            //Register the setup function:
            functions.put(
                "setup",
                new NeoFormConfigConfigurationSpecV1.Function(
                    subsystems.getTools().getInstallerTools().get(),
                    configuration.arguments()
                )
            );

            //And we inject the setup step, at the same index as decompile which will inject it before that.
            steps.add(
                decompileIndex,
                new NeoFormConfigConfigurationSpecV1.Step(
                    "setup", "setup",
                    configuration.values()
                )
            );
        };
    }

    private record SetupConfiguration(
        List<String> arguments,
        Map<String, String> values) {}

    private SetupConfiguration buildSetupConfiguration(final UserdevProfile userDevProfile, final FileTree userDevJar)
    {
        final Decompiler decompilerSubsystemConfiguration = getProject().getExtensions().getByType(Subsystems.class).getDecompiler();
        if (decompilerSubsystemConfiguration.getIsDisabled().get() && useCombinedJarWithNeoForgeOnRecompile(userDevProfile))
        {
            return new SetupConfiguration(
                List.of(
                    "--task", "PROCESS_MINECRAFT_JAR",
                    "--input", "{client}",
                    "--input", "{server}",
                    "--output", "{output}",
                    "--input-mappings", "{clientMappings}",
                    "--neoform-data", "{neoform}",
                    "--apply-patches", "{patches}"
                ),
                Map.of(
                    "client", "{downloadClientOutput}",
                    "clientMappings", "{downloadClientMappingsOutput}",
                    "server", "{downloadServerOutput}",
                    "neoform", "{neoform}",
                    "patches", userDevProfile.getBinaryPatchFile()
                        .map(patchFilePath -> userDevJar
                            .matching(matcher -> matcher.include(patchFilePath))
                            .getSingleFile()).get().getAbsolutePath()
                )
            );
        }

        return new SetupConfiguration(
            List.of(
                "--task", "PROCESS_MINECRAFT_JAR",
                "--input", "{client}",
                "--input", "{server}",
                "--output", "{output}",
                "--input-mappings", "{clientMappings}",
                "--neoform-data", "{neoform}"
            ),
            Map.of(
                "client", "{downloadClientOutput}",
                "clientMappings", "{downloadClientMappingsOutput}",
                "server", "{downloadServerOutput}",
                "neoform", "{neoform}"
            )
        );
    }

    private boolean useCombinedJarWithNeoForgeOnRecompile(final UserdevProfile userDevProfile)
    {
        return userDevProfile.getFeatures()
            .flatMap(UserdevProfile.Features::getUsesCombinedBinaryPatches)
            .getOrElse(false);
    }

    @Override
    protected void afterRegistration(UserDevRuntimeDefinition runtime)
    {
        final RunTypeManager runTypes = getProject().getExtensions().getByType(RunTypeManager.class);
        runtime.getUserdevConfiguration().getRunTypes().forEach((type) -> {
            TypesUtil.registerWithPotentialPrefix(runTypes, runtime.getSpecification().getIdentifier(), type.getName(), type::copyTo);
        });

        final Conventions conventions = getProject().getExtensions().getByType(Subsystems.class).getConventions();
        if (conventions.getIsEnabled().get()
            && conventions.getRuns().getIsEnabled().get()
            && conventions.getRuns().getShouldDefaultRunsBeCreated().get())
        {
            final RunManager runs = getProject().getExtensions().getByType(RunManager.class);
            runtime.getUserdevConfiguration().getRunTypes().forEach(runType -> {
                if (runs.getNames().contains(runType.getName()))
                {
                    return;
                }

                try
                {
                    runs.create(runType.getName());
                }
                catch (IllegalStateException ignored)
                {
                    //thrown when the dependency is added lazily. This is fine.
                }
            });
        }

        //After the project evaluation completes, we can register the binary patch mode if the decompiler is enabled and we support it
        ProjectUtils.afterEvaluate(
            getProject(),
            () -> bakeDefinition(runtime)
        );
    }

    private void bakeDefinition(UserDevRuntimeDefinition definition)
    {
        final Decompiler decompilerSubsystemConfiguration = this.getProject().getExtensions().getByType(Subsystems.class).getDecompiler();
        if (decompilerSubsystemConfiguration.getIsDisabled().get() && useCombinedJarWithNeoForgeOnRecompile(definition.getUserdevConfiguration()))
        {
            definition.getNeoFormRuntimeDefinition().getRawJarTask().configure(task -> {
                task.getInput().set(definition.getNeoFormRuntimeDefinition().getTask("setup").flatMap(WithOutput::getOutput));
            });
        }
    }

    @Override
    protected UserDevRuntimeSpecification.Builder createBuilder()
    {
        return UserDevRuntimeSpecification.Builder.from(getProject());
    }

    private TaskTreeAdapter createPatchAdapter(FileTree userDevArchive, String patchDirectory)
    {
        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> definition.getSpecification()
            .getProject()
            .getTasks()
            .register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "patchUserDev"), Patch.class, task -> {
                task.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                task.getPatchArchive().from(userDevArchive);
                task.getPatchDirectory().set(patchDirectory);
            });
    }

    /*
     * Configures the inject task, which runs right before patching, to also include the content that Neoforge
     * adds to the Minecraft jar, such as the Neoforge sources and resources.
     */
    private void configureNeoforgeInjects(
        InjectZipContent task,
        @Nullable FileTree userDevInjectDir,
        Provider<File> sourcesInjectArtifact,
        Provider<File> resourcesInjectArtifact)
    {

        if (userDevInjectDir != null)
        {
            task.injectFileTree(userDevInjectDir);
        }

        if (sourcesInjectArtifact.isPresent())
        {
            task.injectZip(sourcesInjectArtifact, filter -> {
                filter.include("net/**");
            });
        }

        if (resourcesInjectArtifact.isPresent())
        {
            task.injectZip(resourcesInjectArtifact, filter -> {
                filter.exclude("**/*.class");
                filter.exclude("META-INF/**/*.DSA");
                filter.exclude("**/*.SF");
            });
        }
    }
}
