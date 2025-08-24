package net.neoforged.gradle.neoform.runtime.extensions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.DefaultExecute;
import net.neoforged.gradle.common.runtime.tasks.ListLibraries;
import net.neoforged.gradle.common.tasks.ArtifactFromOutput;
import net.neoforged.gradle.common.util.*;
import net.neoforged.gradle.dsl.common.extensions.ConfigurationData;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.subsystems.*;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.specifications.OutputSpecification;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.specification.NeoFormRuntimeSpecification;
import net.neoforged.gradle.neoform.runtime.tasks.*;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeConstants;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.process.CommandLineArgumentProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"}) // API Design
public abstract class NeoFormRuntimeExtension extends CommonRuntimeExtension<NeoFormRuntimeSpecification, NeoFormRuntimeSpecification.Builder, NeoFormRuntimeDefinition> implements ConfigurableDSLElement<NeoFormRuntimeExtension> {

    private static final Set<String> DISABLED_STEPS = Sets.newHashSet("downloadManifest", "downloadJson");

    @javax.inject.Inject
    public NeoFormRuntimeExtension(Project project) {
        super(project);
    }

    private static void configureMcpRuntimeTaskWithDefaults(NeoFormRuntimeSpecification spec, File neoFormDirectory, Map<String, String> symbolicDataSources, LinkedHashMap<String, TaskProvider<? extends WithOutput>> tasks, NeoFormConfigConfigurationSpecV1.Step step, Runtime neoFormRuntimeTask, Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        buildArguments(neoFormRuntimeTask.getArguments(), spec, step, tasks, neoFormRuntimeTask, alternativeInputProvider);
        configureCommonRuntimeTaskParameters(neoFormRuntimeTask, symbolicDataSources, step.getName(), spec, neoFormDirectory);

        neoFormRuntimeTask.getNeoFormArchive().from(spec.getNeoFormArchive());
    }

    private static void configureMcpRuntimeTaskWithDefaults(NeoFormRuntimeSpecification spec, File neoFormDirectory, Map<String, String> symbolicDataSources, Runtime neoFormRuntimeTask) {
        configureCommonRuntimeTaskParameters(neoFormRuntimeTask, symbolicDataSources, CommonRuntimeUtils.buildStepName(spec, neoFormRuntimeTask.getName()), spec, neoFormDirectory);

        neoFormRuntimeTask.getNeoFormArchive().from(spec.getNeoFormArchive());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable
    private static TaskProvider<? extends WithOutput> createBuiltIn(final NeoFormRuntimeSpecification spec,
                                                                    final NeoFormRuntimeDefinition definition,
                                                                    NeoFormConfigConfigurationSpecV2 neoFormConfigV2,
                                                                    NeoFormConfigConfigurationSpecV1.Step step,
                                                                    final Map<String, TaskProvider<? extends WithOutput>> tasks,
                                                                    final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTaskProviders,
                                                                    final MinecraftArtifactCache cache,
                                                                    final Optional<TaskProvider<? extends WithOutput>> adaptedInput) {
        switch (step.getType()) {
            case "decompile":
                return createDecompile(spec, step, neoFormConfigV2);
            case "downloadClient":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.CLIENT_JAR, a -> {
                    throw new IllegalStateException("Client Jar is required for this step, but was not provided");
                });
            case "downloadServer":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.SERVER_JAR, a -> {
                    throw new IllegalStateException("Server Jar is required for this step, but was not provided");
                });
            case "strip":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), StripJar.class, task -> task.getInput().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task)));
            case "listLibraries":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), ListLibraries.class, task -> {
                    task.getVersionJsonLibraries().from(
                        extractVersionJsonLibraries(spec.getProject(), spec.getMinecraftVersion(), definition)
                    );
                });
            case "inject":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), InjectZipContent.class, task -> {
                    task.getInjectionSource().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task));
                    task.getInjectedSources()
                            .add(task.getRuntimeData().map(data -> data.get("inject"))
                                    .map(inject -> {
                                        final InjectFromFileTreeSource fileTreeSource = task.getObjectFactory()
                                                .newInstance(InjectFromFileTreeSource.class);
                                        fileTreeSource.getFiles().from(inject
                                                .matching(fileTree -> {
                                                    if (spec.getDistribution().equals(DistributionType.SERVER)) {
                                                        fileTree.include("**/server/**");
                                                    } else if (spec.getDistribution().equals(DistributionType.CLIENT)) {
                                                        fileTree.include("**/client/**");
                                                    }
                                                })
                                        );
                                        fileTreeSource.getTreePrefix().set(task.getSymbolicDataSources().map(data -> data.get("inject")));
                                        return fileTreeSource;
                                    })
                            );
                });
            case "patch":
                return spec.getProject().getTasks().register(
                        CommonRuntimeUtils.buildTaskName(spec, step.getName()),
                        Patch.class,
                        task -> {
                            task.getInput().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task));
                            task.getPatchArchive().from(spec.getProject().fileTree(spec.getNeoFormArchive()));
                            task.getPatchDirectory().set(neoFormConfigV2.getData("patches", spec.getDistribution().getName()));
                        }
                );
            case "bundleExtractJar":
                //We handle extraction of the bundle in-house
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.EXTRACTED_SERVER_JAR, a -> {
                    throw new IllegalStateException("Extracted Server Jar is required for this step, but was not provided");
                });
        }
        if (neoFormConfigV2.getSpec() >= 2) {
            switch (step.getType()) {
                case "downloadClientMappings":
                    return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.CLIENT_MAPPINGS, a -> {
                        throw new IllegalStateException("Client Mappings are required for this step, but were not provided");
                    });
                case "downloadServerMappings":
                    return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.SERVER_MAPPINGS, a -> {
                        throw new IllegalStateException("Server Mappings are required for this step, but were not provided");
                    });
            }
        }

        return null;
    }

    @NotNull
    private static TaskProvider<? extends Runtime> createDecompile(NeoFormRuntimeSpecification spec, NeoFormConfigConfigurationSpecV1.Step step, NeoFormConfigConfigurationSpecV2 neoFormConfig) {
        NeoFormConfigConfigurationSpecV1.Function function = neoFormConfig.getFunction(step.getType());
        if (function == null) {
            throw new IllegalArgumentException(String.format("Invalid NeoForm Config, Unknown function step type: %s File: %s", step.getType(), neoFormConfig));
        }

        // Filter out decompiler arguments that aren't related to its outputs (log-level and thread-count)
        List<String> decompilerArgs = new ArrayList<>(function.getArgs());
        decompilerArgs.removeIf(arg -> arg.startsWith("--log-level") || arg.startsWith("-log=") || arg.startsWith("--thread-count") || arg.startsWith("-thr="));

        // Retrieve the default memory size from the JVM arguments configured in NeoForm
        String defaultMaxMemory = "4g";
        List<String> jvmArgs = new ArrayList<>(function.getJvmArgs());
        for (int i = jvmArgs.size() - 1; i >= 0; i--) {
            if (jvmArgs.get(i).startsWith("-Xmx")) {
                defaultMaxMemory = jvmArgs.get(i).substring("-Xmx".length());
                jvmArgs.remove(i);
            }
        }

        // Consider user-settings
        Decompiler settings = spec.getProject().getExtensions().getByType(Subsystems.class).getDecompiler();
        String maxMemory = settings.getMaxMemory().getOrElse(defaultMaxMemory);
        int maxThreads = settings.getMaxThreads().getOrElse(0);
        String logLevel = getDecompilerLogLevelArg(settings.getLogLevel().getOrElse(DecompilerLogLevel.INFO), function.getVersion());

        jvmArgs.addAll(settings.getJvmArgs().get());
        jvmArgs.add("-Xmx" + maxMemory);
        if (maxThreads > 0) {
            decompilerArgs.add(0, "-thr=" + maxThreads);
        }
        decompilerArgs.add(0, "-log=" + logLevel);

        return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), DefaultExecute.class, task -> {
            task.getExecutingJar().set(ToolUtilities.resolveTool(task.getProject(), function.getVersion()));
            task.getJvmArguments().addAll(jvmArgs);
            task.getProgramArguments().addAll(decompilerArgs);
        });
    }

    private static String getDecompilerLogLevelArg(DecompilerLogLevel logLevel, String version) {
        return switch (logLevel) {
            case TRACE -> "trace";
            case INFO -> "info";
            case WARN -> "warn";
            case ERROR -> "error";
            default -> throw new GradleException("LogLevel " + logLevel + " not supported by " + version);
        };
    }

    private TaskProvider<? extends Runtime> createExecute(final NeoFormRuntimeSpecification spec, final NeoFormConfigConfigurationSpecV1.Step step, final NeoFormConfigConfigurationSpecV1.Function function) {
        return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), DefaultExecute.class, task -> {
            task.getExecutingJar().set(ToolUtilities.resolveTool(task.getProject(), function.getVersion()));
            task.getJvmArguments().addAll(function.getJvmArgs());
            task.getProgramArguments().addAll(function.getArgs());
        });
    }

    private static void buildArguments(final RuntimeArguments arguments, final NeoFormRuntimeSpecification spec, NeoFormConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        step.getValues().forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends WithOutput>> dependentTask;
                if (!Objects.equals(key, "input") || alternativeInputProvider.isEmpty()) {
                    dependentTask = NeoFormRuntimeUtils.getInputTaskForTaskFrom(spec, value, tasks);
                } else {
                    dependentTask = alternativeInputProvider;
                }

                dependentTask.ifPresent(taskForArguments::dependsOn);
                dependentTask.ifPresent(task -> arguments.putRegularFile(key, task.flatMap(OutputSpecification::getOutput)));
            } else {
                arguments.put(key, spec.getProject().provider(() -> value));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Map<String, String> buildDataFilesMap(NeoFormConfigConfigurationSpecV2 neoFormConfig, final DistributionType side) {
        return neoFormConfig.getData().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() instanceof Map ? ((Map<String, String>) e.getValue()).get(side.getName()) : (String) e.getValue()
                ));
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @NotNull
    protected NeoFormRuntimeDefinition doCreate(final NeoFormRuntimeSpecification spec) {
        if (this.definitions.containsKey(spec.getIdentifier()))
            throw new IllegalArgumentException("Cannot register runtime with identifier '" + spec.getIdentifier() + "' because it already exists");

        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final MinecraftArtifactCache artifactCache = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);
        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec);

        final Provider<VersionJson> versionJson = artifactCache.cacheVersionManifest(spec.getMinecraftVersion()).map(TransformerUtils.guard(VersionJson::get));

        final Configuration minecraftDependenciesConfiguration = ConfigurationUtils.temporaryUnhandledConfiguration(
                spec.getProject().getConfigurations(),
                "NeoFormMinecraftDependenciesFor" + spec.getIdentifier()
        );

        minecraftDependenciesConfiguration.getDependencies().addAllLater(
                versionJson.map(VersionJson::getLibraries)
                        .map(libraries -> libraries.stream()
                                .map(library -> getProject().getDependencies().create(library.getName()))
                                .collect(Collectors.toList())
                        )
        );

        final File neoFormDirectory = spec.getProject().getExtensions().getByType(ConfigurationData.class)
                .getLocation()
                .dir(String.format("neoForm/%s", spec.getIdentifier())).get().getAsFile();
        final File stepsMcpDirectory = new File(neoFormDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        NeoFormConfigConfigurationSpecV2 neoFormConfig = spec.getConfig();
        neoFormConfig.getLibraries(spec.getDistribution().getName()).forEach(library -> minecraftDependenciesConfiguration.getDependencies().add(
                spec.getProject().getDependencies().create(library)
        ));

        final Map<String, String> symbolicDataSources = buildDataFilesMap(neoFormConfig, spec.getDistribution());

        final TaskProvider<? extends ArtifactFromOutput> sourceJarTask = spec.getProject().getTasks().register("supplySourcesFor" + spec.getIdentifier(), ArtifactFromOutput.class, task -> {
            task.getOutput().set(new File(neoFormDirectory, "sources.jar"));
        });
        final TaskProvider<? extends ArtifactFromOutput> rawJarTask = spec.getProject().getTasks().register("supplyRawJarFor" + spec.getIdentifier(), ArtifactFromOutput.class, task -> {
            task.getOutput().set(new File(neoFormDirectory, "raw.jar"));
        });

        return new NeoFormRuntimeDefinition(
                spec,
                new LinkedHashMap<>(),
                sourceJarTask,
                rawJarTask,
                gameArtifactTasks,
                minecraftDependenciesConfiguration,
                taskProvider -> taskProvider.configure(runtimeTask -> {
                    configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, runtimeTask);
                }),
                versionJson,
                neoFormConfig,
                createDownloadAssetsTasks(spec, versionJson),
                createExtractNativesTasks(spec, symbolicDataSources, neoFormDirectory, versionJson)
        );
    }

    @Override
    protected void afterRegistration(NeoFormRuntimeDefinition runtime) {
        //TODO: Right now this is needed so that runs and other components can be order free in the buildscript,
        //TODO: We should consider making this somehow lazy and remove the unneeded complexity because of it.
        ProjectUtils.afterEvaluate(runtime.getSpecification().getProject(), () -> this.bakeDefinition(runtime));
    }

    @Override
    protected NeoFormRuntimeSpecification.Builder createBuilder() {
        return NeoFormRuntimeSpecification.Builder.from(getProject());
    }

    protected void bakeDefinition(NeoFormRuntimeDefinition definition) {
        final NeoFormRuntimeSpecification spec = definition.getSpecification();
        final NeoFormConfigConfigurationSpecV2 neoFormConfig = definition.getNeoFormConfig();

        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final File neoFormDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("neoForm/%s", spec.getIdentifier())).get().getAsFile();
        final File stepsMcpDirectory = new File(neoFormDirectory, "steps");

        final Map<String, String> versionData = Maps.newHashMap(mappingsExtension.getVersion().get());
        versionData.put(NamingConstants.Version.MINECRAFT_VERSION, spec.getMinecraftVersion());
        versionData.put(NeoFormRuntimeConstants.Naming.Version.NEOFORM_VERSION, spec.getVersionedName());
        definition.setMappingVersionData(versionData);

        final Map<String, String> symbolicDataSources = buildDataFilesMap(neoFormConfig, spec.getDistribution());

        final List<NeoFormConfigConfigurationSpecV1.Step> steps = new ArrayList<>(neoFormConfig.getSteps(spec.getDistribution().getName()));
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Unknown side: " + spec.getDistribution() + " for NeoForm " + definition.getSpecification().getNeoFormVersion());
        }

        steps.removeIf(step -> DISABLED_STEPS.contains(step.getType()));

        final LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs = definition.getTasks();
        for (NeoFormConfigConfigurationSpecV1.Step step : steps) {
            Optional<TaskProvider<? extends WithOutput>> adaptedInput = Optional.empty();

            if (spec.getPreTaskTypeAdapters().containsKey(step.getName())) {
                adaptedInput = adaptPreTaskInput(definition, step, spec, taskOutputs, neoFormDirectory, symbolicDataSources, adaptedInput);
            }

            TaskProvider<? extends WithOutput> neoFormRuntimeTaskProvider = createBuiltIn(
                    spec,
                    definition,
                    neoFormConfig,
                    step,
                    taskOutputs,
                    definition.getGameArtifactProvidingTasks(),
                    artifactCacheExtension,
                    adaptedInput
            );

            if (neoFormRuntimeTaskProvider == null) {
                NeoFormConfigConfigurationSpecV1.Function function = neoFormConfig.getFunction(step.getType());
                if (function == null) {
                    throw new IllegalArgumentException(String.format("Invalid MCP Config, Unknown function step type: %s File: %s", step.getType(), neoFormConfig));
                }

                neoFormRuntimeTaskProvider = createExecute(spec, step, function);

                if (step.getType().equals("mergeMappings")) {
                    neoFormRuntimeTaskProvider.configure(tsk -> tsk.getOutputFileName().set("outputs.tsrg"));
                }
            }

            Optional<TaskProvider<? extends WithOutput>> finalAdaptedInput = adaptedInput;
            neoFormRuntimeTaskProvider.configure((WithOutput neoFormRuntimeTask) -> {
                if (neoFormRuntimeTask instanceof Runtime runtimeTask) {
                    configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, taskOutputs, step, runtimeTask, finalAdaptedInput);
                }
            });

            final String taskName = neoFormRuntimeTaskProvider.getName();
            if (!spec.getPostTypeAdapters().containsKey(step.getName())) {
                taskOutputs.put(taskName, neoFormRuntimeTaskProvider);
            } else {
                for (TaskTreeAdapter taskTreeAdapter : spec.getPostTypeAdapters().get(step.getName())) {
                    final TaskProvider<? extends Runtime> taskProvider = taskTreeAdapter.adapt(definition, neoFormRuntimeTaskProvider, neoFormDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), dependentTaskProvider -> dependentTaskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task)));
                    if (taskProvider != null) {
                        taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task));
                        neoFormRuntimeTaskProvider = taskProvider;
                    }
                }

                // We consider the outputs of the final post adapter the outputs of step
                taskOutputs.put(taskName, neoFormRuntimeTaskProvider);
                taskOutputs.put(neoFormRuntimeTaskProvider.getName(), neoFormRuntimeTaskProvider);
            }
        }

        final TaskProvider<? extends WithOutput> lastTask = Iterators.getLast(taskOutputs.values().iterator());
        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = Sets.newHashSet();

        TaskProvider<? extends WithOutput> recompileInput = adaptPreTaskInput(
                definition,
                "recompile",
                spec,
                neoFormDirectory,
                symbolicDataSources,
                Optional.of(lastTask),
                Optional.of(lastTask)
        ).orElseThrow(() -> new IllegalStateException("No input for recompile task due to pre-task adapters"));

        final FileCollection recompileDependencies = definition.getAdditionalRecompileDependencies().plus(spec.getProject().files(definition.getMinecraftDependenciesConfiguration()));

        final TaskProvider<? extends Runtime> recompileTask =
            createRecompileTask(definition, recompileInput, spec, recompileDependencies, task -> {
                task.configure(neoFormRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, neoFormRuntimeTask));
            });

        taskOutputs.put(recompileTask.getName(), recompileTask);

        definition.getSourceJarTask().configure(task -> {
            task.getInput().set(recompileInput.flatMap(WithOutput::getOutput));
        });
        definition.getRawJarTask().configure(task -> {
            task.getInput().set(recompileTask.flatMap(WithOutput::getOutput));
        });
    }

    private @NotNull TaskProvider<? extends Runtime> createRecompileTask(
        final NeoFormRuntimeDefinition definition,
        final TaskProvider<? extends WithOutput> recompileInput,
        final NeoFormRuntimeSpecification spec,
        final FileCollection recompileDependencies,
        final Consumer<TaskProvider<? extends Runtime>> configure)
    {
        Recompiler settings = spec.getProject().getExtensions().getByType(Subsystems.class).getRecompiler();

        return createGradleRecompileTask(definition, recompileInput, spec, recompileDependencies, configure);
    }

    private @NotNull TaskProvider<InProcessSourceJarRecompiler> createNativeRecompileTask(
        final NeoFormRuntimeDefinition definition,
        final TaskProvider<? extends WithOutput> recompileInput,
        final NeoFormRuntimeSpecification spec,
        final FileCollection recompileDependencies,
        final Consumer<TaskProvider<? extends Runtime>> configure)
    {
        final TaskProvider<InProcessSourceJarRecompiler> compiler = spec.getProject()
            .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), InProcessSourceJarRecompiler.class, task -> {
                task.getSourceToCompile().set(recompileInput.flatMap(WithOutput::getOutput));
                task.getClasspath().from(recompileDependencies);
                task.getAdditionalSources().from(definition.getAdditionalCompileSources());
                task.getResources().from(recompileInput.flatMap(WithOutput::getOutput).map(task.getArchiveOperations()::zipTree).map(zipTree -> zipTree.matching(sp -> sp.exclude("**/*.java"))));
            });

        configure.accept(compiler);

        return compiler;
    }

    private @NotNull TaskProvider<RecompileSourceJar> createGradleRecompileTask(
        final NeoFormRuntimeDefinition definition,
        final TaskProvider<? extends WithOutput> recompileInput,
        final NeoFormRuntimeSpecification spec,
        final FileCollection recompileDependencies,
        final Consumer<TaskProvider<? extends Runtime>> configure)
    {
        final Provider<? extends FileTree> recompileSourceFileTree = recompileInput.flatMap(WithOutput::getOutputAsTree);

        // Consider user-settings
        final TaskProvider<RecompileSourceJar> recompileSourceJar = spec.getProject()
                .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, task -> {
                    task.getCompileFileRoot().from(recompileSourceFileTree);
                    task.getAdditionalInputFileRoot().from(definition.getAdditionalCompileSources());
                    task.getAdditionalInputFileRoot().from(getProject().file(recompileInput.flatMap(WithOutput::getOutput)));
                    task.setClasspath(recompileDependencies);
                    task.getStepName().set("recompile");

                    // Consider user-settings
                    Recompiler settings = spec.getProject().getExtensions().getByType(Subsystems.class).getRecompiler();
                    String maxMemory = settings.getMaxMemory().get();
                    task.getOptions().setFork(settings.getShouldFork().get());
                    ForkOptions forkOptions = task.getOptions().getForkOptions();
                    forkOptions.setMemoryMaximumSize(maxMemory);
                    forkOptions.setJvmArgs(settings.getJvmArgs().get());
                    task.getOptions().getCompilerArgumentProviders().add(new CustomCompilerArgsProvider(settings.getArgs()));

                    task.getResources().from(recompileInput.flatMap(WithOutput::getOutputAsTree).map(zipTree -> zipTree.matching(sp -> sp.exclude("**/*.java"))));
                });

        configure.accept(recompileSourceJar);

        return recompileSourceJar;
    }

    private static Optional<TaskProvider<? extends WithOutput>> adaptPreTaskInput(NeoFormRuntimeDefinition definition, NeoFormConfigConfigurationSpecV1.Step step, NeoFormRuntimeSpecification spec, LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs, File neoFormDirectory, Map<String, String> symbolicDataSources, Optional<TaskProvider<? extends WithOutput>> adaptedInput) {
        final String inputArgumentMarker = step.getValue("input");
        if (inputArgumentMarker == null) {
            throw new IllegalStateException("Can not change input chain on: " + step.getName() + " it has no input to transform!");
        }

        Optional<TaskProvider<? extends WithOutput>> inputTask = NeoFormRuntimeUtils.getInputTaskForTaskFrom(spec, inputArgumentMarker, taskOutputs);

        return adaptPreTaskInput(definition, step.getName(), spec, neoFormDirectory, symbolicDataSources, adaptedInput, inputTask);
    }

    private static Optional<TaskProvider<? extends WithOutput>> adaptPreTaskInput(
        NeoFormRuntimeDefinition definition,
        String stepName,
        NeoFormRuntimeSpecification spec,
        File neoFormDirectory,
        Map<String, String> symbolicDataSources,
        Optional<TaskProvider<? extends WithOutput>> adaptedInput,
        Optional<TaskProvider<? extends WithOutput>> inputTask) {
        if (!spec.getPreTaskTypeAdapters().get(stepName).isEmpty() && inputTask.isPresent()) {
            for (TaskTreeAdapter taskTreeAdapter : spec.getPreTaskTypeAdapters().get(stepName)) {
                final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(definition, inputTask.get(), neoFormDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task)));
                if (modifiedTree != null) {
                    modifiedTree.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task));
                    inputTask = Optional.of(modifiedTree);
                }
            }

            adaptedInput = inputTask;
        }
        return adaptedInput;
    }

    public record CustomCompilerArgsProvider(Provider<List<String>> args) implements CommandLineArgumentProvider {
        @Override
        public Iterable<String> asArguments() {
            return args.get();
        }
    }
}
