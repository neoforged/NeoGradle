package net.neoforged.gradle.neoform.runtime.extensions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.common.runtime.tasks.ListLibraries;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.extensions.ConfigurationData;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Decompiler;
import net.neoforged.gradle.dsl.common.extensions.subsystems.DecompilerLogLevel;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Parchment;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Recompiler;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
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
import net.neoforged.gradle.neoform.runtime.tasks.InjectFromFileTreeSource;
import net.neoforged.gradle.neoform.runtime.tasks.InjectZipContent;
import net.neoforged.gradle.neoform.runtime.tasks.PackZip;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.neoform.runtime.tasks.RecompileSourceJar;
import net.neoforged.gradle.neoform.runtime.tasks.StripJar;
import net.neoforged.gradle.neoform.runtime.tasks.UnpackJavaSources;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeConstants;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.ForkOptions;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
                    task.getDownloadedVersionJsonFile()
                                    .fileProvider(task.newProvider(cache.cacheVersionManifest(spec.getMinecraftVersion())));
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
                            task.getPatchArchive().from(spec.getNeoFormArchive());
                            task.getPatchDirectory().set(neoFormConfigV2.getData("patches", spec.getDistribution().getName()));
                        }
                );
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

        // Filter out decompiler arguments that aren't related to its output (log-level and thread-count)
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

        return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            task.getExecutingJar().set(ToolUtilities.resolveTool(task.getProject(), function.getVersion()));
            task.getJvmArguments().addAll(jvmArgs);
            task.getProgramArguments().addAll(decompilerArgs);
        });
    }

    private static String getDecompilerLogLevelArg(DecompilerLogLevel logLevel, String version) {
        switch (logLevel) {
            case TRACE:
                return "trace";
            case INFO:
                return "info";
            case WARN:
                return "warn";
            case ERROR:
                return "error";
            default:
                throw new GradleException("LogLevel " + logLevel + " not supported by " + version);
        }
    }

    private TaskProvider<? extends Runtime> createExecute(final NeoFormRuntimeSpecification spec, final NeoFormConfigConfigurationSpecV1.Step step, final NeoFormConfigConfigurationSpecV1.Function function) {
        return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            task.getExecutingJar().set(ToolUtilities.resolveTool(task.getProject(), function.getVersion()));
            task.getJvmArguments().addAll(function.getJvmArgs());
            task.getProgramArguments().addAll(function.getArgs());
        });
    }

    private static void buildArguments(final RuntimeArguments arguments, final NeoFormRuntimeSpecification spec, NeoFormConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        step.getValues().forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends WithOutput>> dependentTask;
                if (!Objects.equals(key, "input") || !alternativeInputProvider.isPresent()) {
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
        if (this.runtimes.containsKey(spec.getIdentifier()))
            throw new IllegalArgumentException("Cannot register runtime with identifier '" + spec.getIdentifier() + "' because it already exists");

        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final Map<GameArtifact, File> gameArtifacts = artifactCacheExtension.cacheGameVersion(spec.getMinecraftVersion(), spec.getDistribution());

        final VersionJson versionJson;
        try {
            versionJson = VersionJson.get(gameArtifacts.get(GameArtifact.VERSION_MANIFEST));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read VersionJson from the launcher metadata for the minecraft version: %s", spec.getMinecraftVersion()), e);
        }

        final Configuration minecraftDependenciesConfiguration = spec.getProject().getConfigurations().detachedConfiguration();
        minecraftDependenciesConfiguration.setCanBeResolved(true);
        minecraftDependenciesConfiguration.setCanBeConsumed(false);
        for (VersionJson.Library library : versionJson.getLibraries()) {
            minecraftDependenciesConfiguration.getDependencies().add(
                    spec.getProject().getDependencies().create(library.getName())
            );
        }

        final File neoFormDirectory = spec.getProject().getExtensions().getByType(ConfigurationData.class)
                .getLocation()
                .dir(String.format("neoForm/%s", spec.getIdentifier())).get().getAsFile();
        final File stepsMcpDirectory = new File(neoFormDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        NeoFormConfigConfigurationSpecV2 neoFormConfig = spec.getConfig();
        neoFormConfig.getLibraries(spec.getDistribution().getName()).forEach(library -> minecraftDependenciesConfiguration.getDependencies().add(
                spec.getProject().getDependencies().create(library)
        ));

        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec);

        final Map<String, String> symbolicDataSources = buildDataFilesMap(neoFormConfig, spec.getDistribution());

        final TaskProvider<? extends ArtifactProvider> sourceJarTask = spec.getProject().getTasks().register("supplySourcesFor" + spec.getIdentifier(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(neoFormDirectory, "sources.jar"));
        });
        final TaskProvider<? extends ArtifactProvider> rawJarTask = spec.getProject().getTasks().register("supplyRawJarFor" + spec.getIdentifier(), ArtifactProvider.class, task -> {
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
                createDownloadAssetsTasks(spec, symbolicDataSources, neoFormDirectory, versionJson),
                createExtractNativesTasks(spec, symbolicDataSources, neoFormDirectory, versionJson)
        );
    }

    @Override
    protected NeoFormRuntimeSpecification.Builder createBuilder() {
        return NeoFormRuntimeSpecification.Builder.from(getProject());
    }

    @Override
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
                final String inputArgumentMarker = step.getValue("input");
                if (inputArgumentMarker == null) {
                    throw new IllegalStateException("Can not change input chain on: " + step.getName() + " it has no input to transform!");
                }

                Optional<TaskProvider<? extends WithOutput>> inputTask = NeoFormRuntimeUtils.getInputTaskForTaskFrom(spec, inputArgumentMarker, taskOutputs);

                if (!spec.getPreTaskTypeAdapters().get(step.getName()).isEmpty() && inputTask.isPresent()) {
                    for (TaskTreeAdapter taskTreeAdapter : spec.getPreTaskTypeAdapters().get(step.getName())) {
                        final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(definition, inputTask.get(), neoFormDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task)));
                        if (modifiedTree != null) {
                            modifiedTree.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task));
                            inputTask = Optional.of(modifiedTree);
                        }
                    }

                    adaptedInput = inputTask;
                }
            }

            TaskProvider<? extends WithOutput> neoFormRuntimeTaskProvider = createBuiltIn(
                    spec,
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
            }

            Optional<TaskProvider<? extends WithOutput>> finalAdaptedInput = adaptedInput;
            neoFormRuntimeTaskProvider.configure((WithOutput neoFormRuntimeTask) -> {
                if (neoFormRuntimeTask instanceof Runtime) {
                    final Runtime runtimeTask = (Runtime) neoFormRuntimeTask;
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

                // We consider the output of the final post adapter the output of step
                taskOutputs.put(taskName, neoFormRuntimeTaskProvider);
                taskOutputs.put(neoFormRuntimeTaskProvider.getName(), neoFormRuntimeTaskProvider);
            }
        }

        final TaskProvider<? extends WithOutput> lastTask = Iterators.getLast(taskOutputs.values().iterator());
        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                spec.getProject(), String.format("mapGameFor%s", StringUtils.capitalize(spec.getVersionedName())), taskName -> CommonRuntimeUtils.buildTaskName(spec, taskName), lastTask, definition.getGameArtifactProvidingTasks(), versionData, additionalRuntimeTasks, definition
        );

        final TaskProvider<? extends Runtime> remapTask = context.getNamingChannel().getApplySourceMappingsTaskBuilder().get().build(context);
        additionalRuntimeTasks.forEach(taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task)));
        remapTask.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, task));

        Provider<RegularFile> recompileInput = maybeApplyParchment(
                spec,
                remapTask.flatMap(WithOutput::getOutput),
                symbolicDataSources,
                neoFormDirectory,
                context.getLibrariesTask().flatMap(WithOutput::getOutput)
        );

        final FileCollection recompileDependencies = spec.getAdditionalRecompileDependencies().plus(spec.getProject().files(definition.getMinecraftDependenciesConfiguration()));
        final TaskProvider<UnpackJavaSources> unpackSources = spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "unzipSources"), UnpackJavaSources.class, task -> {
            task.getInputZip().set(recompileInput);
        });
        unpackSources.configure(neoFormRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, neoFormRuntimeTask));

        final TaskProvider<RecompileSourceJar> recompileTask = spec.getProject()
                .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, task -> {
                    task.setSource(unpackSources.flatMap(UnpackJavaSources::getUnpackingTarget));
                    task.getCompileClasspath().setFrom(recompileDependencies);
                    task.getStepName().set("recompile");

                    // Consider user-settings
                    Recompiler settings = spec.getProject().getExtensions().getByType(Subsystems.class).getRecompiler();
                    String maxMemory = settings.getMaxMemory().get();
                    ForkOptions forkOptions = task.getOptions().getForkOptions();
                    forkOptions.setMemoryMaximumSize(maxMemory);
                    forkOptions.setJvmArgs(settings.getJvmArgs().get());
                    task.getOptions().getCompilerArgumentProviders().add(settings.getArgs()::get);
                });

        recompileTask.configure(neoFormRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, neoFormRuntimeTask));

        final TaskProvider<PackZip> packTask = spec.getProject()
                        .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "packRecomp"), PackZip.class, task -> {
                            task.getInputFiles().from(recompileTask.flatMap(AbstractCompile::getDestinationDirectory));
                            task.getInputFiles().from(getProject().fileTree(recompileInput)
                                    .matching(sp -> sp.exclude("**/*.java")));
                });

        packTask.configure(neoFormRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, symbolicDataSources, neoFormRuntimeTask));

        taskOutputs.put(recompileTask.getName(), packTask);

        definition.getSourceJarTask().configure(task -> {
            task.getInputFiles().from(recompileInput);
            task.dependsOn(remapTask);
        });
        definition.getRawJarTask().configure(task -> {
            task.getInputFiles().from(packTask.flatMap(WithOutput::getOutput));
            task.dependsOn(packTask);
        });
    }

    private static Provider<RegularFile> maybeApplyParchment(NeoFormRuntimeSpecification spec,
                                                             Provider<RegularFile> recompileInput,
                                                             Map<String, String> symbolicDataSources,
                                                             File neoFormDirectory,
                                                             Provider<RegularFile> listLibrariesOutput) {
        Project project = spec.getProject();
        Parchment parchment = project.getExtensions().getByType(Subsystems.class).getParchment();
        if (!parchment.getEnabled().get()) {
            return recompileInput;
        }

        TaskProvider<? extends Runtime> applyParchmentTask = project.getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "applyParchment"), Execute.class, task -> {
            // Provide the mappings via artifact
            File mappingFile = ToolUtilities.resolveTool(project, parchment.getParchmentArtifact().get());
            File toolExecutable = ToolUtilities.resolveTool(project, parchment.getToolArtifact().get());

            task.getInputs().file(mappingFile);
            task.getInputs().file(recompileInput);
            task.getInputs().file(listLibrariesOutput);
            task.getExecutingJar().set(toolExecutable);
            task.getProgramArguments().add(listLibrariesOutput.map(f -> "--libraries-list=" + f.getAsFile().getAbsolutePath()));
            task.getProgramArguments().add("--enable-parchment");
            task.getProgramArguments().add("--parchment-mappings=" + mappingFile.getAbsolutePath());
            task.getProgramArguments().add("--in-format=archive");
            task.getProgramArguments().add("--out-format=archive");
            task.getProgramArguments().add(recompileInput.map(f -> f.getAsFile().getAbsolutePath()));
            task.getProgramArguments().add("{output}");
            configureCommonRuntimeTaskParameters(task, symbolicDataSources, "applyParchment", spec, neoFormDirectory);
        });

        return applyParchmentTask.flatMap(WithOutput::getOutput);
    }
}
