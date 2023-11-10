package net.neoforged.gradle.neoform.runtime.extensions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.common.runtime.tasks.ListLibraries;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.specifications.OutputSpecification;
import net.neoforged.gradle.dsl.common.util.*;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.specification.NeoFormRuntimeSpecification;
import net.neoforged.gradle.neoform.runtime.tasks.InjectCode;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.neoform.runtime.tasks.RecompileSourceJar;
import net.neoforged.gradle.neoform.runtime.tasks.StripJar;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeConstants;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"}) // API Design
public abstract class NeoFormRuntimeExtension extends CommonRuntimeExtension<NeoFormRuntimeSpecification, NeoFormRuntimeSpecification.Builder, NeoFormRuntimeDefinition> {

    @javax.inject.Inject
    public NeoFormRuntimeExtension(Project project) {
        super(project);
    }

    private static void configureMcpRuntimeTaskWithDefaults(NeoFormRuntimeSpecification spec, File neoFormDirectory, Map<String, File> dataFiles, Map<String, File> dataDirectories, LinkedHashMap<String, TaskProvider<? extends WithOutput>> tasks, NeoFormConfigConfigurationSpecV1.Step step, Runtime neoFormRuntimeTask, Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        buildArguments(neoFormRuntimeTask.getArguments(), spec, step, tasks, neoFormRuntimeTask, alternativeInputProvider);
        configureCommonRuntimeTaskParameters(neoFormRuntimeTask, dataFiles, dataDirectories, step.getName(), spec, neoFormDirectory);
    }

    private static void configureMcpRuntimeTaskWithDefaults(NeoFormRuntimeSpecification spec, File neoFormDirectory, Map<String, File> dataFiles, Map<String, File> dataDirectories, Runtime neoFormRuntimeTask) {
        configureCommonRuntimeTaskParameters(neoFormRuntimeTask, dataFiles, dataDirectories, CommonRuntimeUtils.buildStepName(spec, neoFormRuntimeTask.getName()), spec, neoFormDirectory);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable
    private static TaskProvider<? extends WithOutput> createBuiltIn(final NeoFormRuntimeSpecification spec, NeoFormConfigConfigurationSpecV2 neoFormConfigV2, NeoFormConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTaskProviders, final Optional<TaskProvider<? extends WithOutput>> adaptedInput) {
        switch (step.getType()) {
            case "downloadManifest":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.LAUNCHER_MANIFEST, a -> {
                    throw new IllegalStateException("Launcher Manifest is required for this step, but was not provided");
                });
            case "downloadJson":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.VERSION_MANIFEST, a -> {
                    throw new IllegalStateException("Version Manifest is required for this step, but was not provided");
                });
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
                    task.getDownloadedVersionJsonFile().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput, task));
                });
            case "inject":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), InjectCode.class, task -> {
                    task.getInjectionSource().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task));
                    if (spec.getDistribution().equals(DistributionType.SERVER)) {
                        task.getInclusionFilter().add("**/server/**");
                    } else if (spec.getDistribution().equals(DistributionType.CLIENT)) {
                        task.getInclusionFilter().add("**/client/**");
                    }
                });
            case "patch":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Patch.class, task -> task.getInput().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task)));
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

    private static TaskProvider<? extends Runtime> createExecute(final NeoFormRuntimeSpecification spec, final NeoFormConfigConfigurationSpecV1.Step step, final NeoFormConfigConfigurationSpecV1.Function function) {
        Project project = spec.getProject();

        return project.getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            String version = function.getVersion();
            List<String> jvmArgs = new ArrayList<>(function.getJvmArgs());
            List<String> args = new ArrayList<>(function.getArgs());
            Map<UserOverride, String> userOverrides = getUserOverrides(spec, step.getName(),
                    UserOverride.MAX_MEMORY, UserOverride.VERSION);

            // Allow the user to override the specific tool version per step
            version = userOverrides.getOrDefault(UserOverride.VERSION, version);

            // Allow the user to override the maximum memory per step
            String maxMemoryOverride = userOverrides.get(UserOverride.MAX_MEMORY);
            if (maxMemoryOverride != null) {
                // Filter out existing memory options and append new max memory setting
                jvmArgs = Stream.concat(
                        jvmArgs.stream().filter(s -> !s.startsWith("-Xmx")),
                        Stream.of("-Xmx" + maxMemoryOverride)
                ).collect(Collectors.toList());
            }
            appendUserOverridesToDescription(task, userOverrides);

            task.getExecutingJar().set(ToolUtilities.resolveTool(task.getProject(), version));
            task.getJvmArguments().addAll(jvmArgs);
            task.getProgramArguments().addAll(args);
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

    @NotNull
    private static Map<String, File> buildDataFilesMap(NeoFormConfigConfigurationSpecV2 neoFormConfig, final DistributionType side, final File unpackedMcpDirectory) {
        final Map<String, File> dataMap = buildDataMap(neoFormConfig, side, unpackedMcpDirectory);
        final Map<String, File> dataFiles = Maps.newHashMap();
        dataMap.forEach((key, value) -> {
            if (value.isFile()) {
                dataFiles.put(key, value);
            }
        });
        
        return dataFiles;
    }
    
    @NotNull
    private static Map<String, File> buildDataDirectoriesMap(NeoFormConfigConfigurationSpecV2 neoFormConfig, final DistributionType side, final File unpackedMcpDirectory) {
        final Map<String, File> dataMap = buildDataMap(neoFormConfig, side, unpackedMcpDirectory);
        final Map<String, File> dataFiles = Maps.newHashMap();
        dataMap.forEach((key, value) -> {
            if (value.isDirectory()) {
                dataFiles.put(key, value);
            }
        });
        
        return dataFiles;
    }
    
    @SuppressWarnings("unchecked")
    @NotNull
    private static Map<String, File> buildDataMap(NeoFormConfigConfigurationSpecV2 neoFormConfig, final DistributionType side, final File unpackedMcpDirectory) {
        return neoFormConfig.getData().entrySet().stream()
                       .collect(Collectors.toMap(Map.Entry::getKey,
                               e -> new File(unpackedMcpDirectory, e.getValue() instanceof Map ? ((Map<String, String>) e.getValue()).get(side.getName()) : (String) e.getValue())
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
        final Dependency neoFormConfigDependency = spec.getNeoFormArtifact().toDependency(spec.getProject());
        final Configuration neoFormDownloadConfiguration = ConfigurationUtils.temporaryConfiguration(spec.getProject(), neoFormConfigDependency);
        final ResolvedConfiguration resolvedConfiguration = neoFormDownloadConfiguration.getResolvedConfiguration();
        final File neoFormZipFile = resolvedConfiguration.getFiles().iterator().next();

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final Map<GameArtifact, File> gameArtifacts = artifactCacheExtension.cacheGameVersion(spec.getMinecraftVersion(), spec.getDistribution());

        final VersionJson versionJson;
        try {
            versionJson = VersionJson.get(gameArtifacts.get(GameArtifact.VERSION_MANIFEST));
        } catch (FileNotFoundException e) {
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

        final File neoFormDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("neoForm/%s", spec.getIdentifier())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(neoFormDirectory, "unpacked");
        final File stepsMcpDirectory = new File(neoFormDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        final FileTree neoFormZipFileTree = spec.getProject().zipTree(neoFormZipFile);
        final CopyingFileTreeVisitor unpackingVisitor = new CopyingFileTreeVisitor(unpackedMcpZipDirectory);
        neoFormZipFileTree.visit(unpackingVisitor);

        final File neoFormConfigFile = new File(unpackedMcpZipDirectory, "config.json");
        final NeoFormConfigConfigurationSpecV2 neoFormConfig = NeoFormConfigConfigurationSpecV2.get(neoFormConfigFile);

        neoFormConfig.getLibraries(spec.getDistribution().getName()).forEach(library -> minecraftDependenciesConfiguration.getDependencies().add(
                spec.getProject().getDependencies().create(library)
        ));

        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec);

        final Map<String, File> dataFiles = buildDataFilesMap(neoFormConfig, spec.getDistribution(), unpackedMcpZipDirectory);
        final Map<String, File> dataDirectories = buildDataDirectoriesMap(neoFormConfig, spec.getDistribution(), unpackedMcpZipDirectory);

        final TaskProvider<? extends ArtifactProvider> sourceJarTask = spec.getProject().getTasks().register("supplySourcesFor" + spec.getIdentifier(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(neoFormDirectory, "sources.jar"));
        });
        final TaskProvider<? extends ArtifactProvider> rawJarTask = spec.getProject().getTasks().register("supplyRawJarFor" + spec.getIdentifier(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(neoFormDirectory, "raw.jar"));
        });
        
        return new NeoFormRuntimeDefinition(spec, new LinkedHashMap<>(), sourceJarTask, rawJarTask, gameArtifactTasks, minecraftDependenciesConfiguration, taskProvider -> taskProvider.configure(runtimeTask -> {
            configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, runtimeTask);
        }), versionJson, unpackedMcpZipDirectory, neoFormConfig, createDownloadAssetsTasks(spec, dataFiles, dataDirectories, neoFormDirectory, versionJson), createExtractNativesTasks(spec, dataFiles, dataDirectories, neoFormDirectory, versionJson));
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
        final File unpackedMcpZipDirectory = new File(neoFormDirectory, "unpacked");
        final File stepsMcpDirectory = new File(neoFormDirectory, "steps");

        final Map<String, String> versionData = Maps.newHashMap(mappingsExtension.getVersion().get());
        versionData.put(NamingConstants.Version.MINECRAFT_VERSION, spec.getMinecraftVersion());
        versionData.put(NeoFormRuntimeConstants.Naming.Version.NEOFORM_VERSION, spec.getVersionedName());
        definition.setMappingVersionData(versionData);

        final Map<String, File> dataFiles = buildDataFilesMap(neoFormConfig, spec.getDistribution(), unpackedMcpZipDirectory);
        final Map<String, File> dataDirectories = buildDataDirectoriesMap(neoFormConfig, spec.getDistribution(), unpackedMcpZipDirectory);

        final List<NeoFormConfigConfigurationSpecV1.Step> steps = neoFormConfig.getSteps(spec.getDistribution().getName());
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Unknown side: " + spec.getDistribution() + " For Config: " + definition.getSpecification().getNeoFormArtifact());
        }

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
                        final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(definition, inputTask.get(), neoFormDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, task)));
                        if (modifiedTree != null) {
                            modifiedTree.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, task));
                            inputTask = Optional.of(modifiedTree);
                        }
                    }

                    adaptedInput = inputTask;
                }
            }

            TaskProvider<? extends WithOutput> neoFormRuntimeTaskProvider = createBuiltIn(spec, neoFormConfig, step, taskOutputs, definition.getGameArtifactProvidingTasks(), adaptedInput);

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
                    configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, taskOutputs, step, runtimeTask, finalAdaptedInput);
                }
            });

            if (!spec.getPostTypeAdapters().containsKey(step.getName())) {
                taskOutputs.put(neoFormRuntimeTaskProvider.getName(), neoFormRuntimeTaskProvider);
            } else {
                for (TaskTreeAdapter taskTreeAdapter : spec.getPostTypeAdapters().get(step.getName())) {
                    final TaskProvider<? extends Runtime> taskProvider = taskTreeAdapter.adapt(definition, neoFormRuntimeTaskProvider, neoFormDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), dependentTaskProvider -> dependentTaskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, task)));
                    if (taskProvider != null) {
                        taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, task));
                        neoFormRuntimeTaskProvider = taskProvider;
                    }
                }

                taskOutputs.put(neoFormRuntimeTaskProvider.getName(), neoFormRuntimeTaskProvider);
            }
        }

        final TaskProvider<? extends WithOutput> lastTask = Iterators.getLast(taskOutputs.values().iterator());
        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                spec.getProject(), String.format("mapGameFor%s", StringUtils.capitalize(spec.getVersionedName())), taskName -> CommonRuntimeUtils.buildTaskName(spec, taskName), lastTask, definition.getGameArtifactProvidingTasks(), versionData, additionalRuntimeTasks, definition
        );

        final TaskProvider<? extends Runtime> remapTask = context.getNamingChannel().getApplySourceMappingsTaskBuilder().get().build(context);
        additionalRuntimeTasks.forEach(taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, task)));
        remapTask.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, task));

        final FileCollection recompileDependencies = spec.getAdditionalRecompileDependencies().plus(spec.getProject().files(definition.getMinecraftDependenciesConfiguration()));
        final TaskProvider<? extends Runtime> recompileTask = spec.getProject()
                .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, recompileSourceJar -> {
                    recompileSourceJar.getInputJar().set(remapTask.flatMap(WithOutput::getOutput));
                    recompileSourceJar.getCompileClasspath().setFrom(recompileDependencies);
                    recompileSourceJar.getStepName().set("recompile");

                    // Allow user-overrides for the recompile task
                    Map<UserOverride, String> userOverrides = getUserOverrides(spec, "recompile", UserOverride.MAX_MEMORY);
                    String maxMemoryOverride = userOverrides.get(UserOverride.MAX_MEMORY);
                    if (maxMemoryOverride != null) {
                        recompileSourceJar.getOptions().getForkOptions().setMemoryMaximumSize(maxMemoryOverride);
                    }
                    appendUserOverridesToDescription(recompileSourceJar, userOverrides);
                });
        recompileTask.configure(neoFormRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, neoFormDirectory, dataFiles, dataDirectories, neoFormRuntimeTask));

        taskOutputs.put(recompileTask.getName(), recompileTask);

        definition.getSourceJarTask().configure(task -> {
            task.getInput().set(remapTask.flatMap(WithOutput::getOutput));
            task.dependsOn(remapTask);
        });
        definition.getRawJarTask().configure(task -> {
            task.getInput().set(recompileTask.flatMap(WithOutput::getOutput));
            task.dependsOn(recompileTask);
        });
    }

    /**
     * Get the map of property overrides set by the user for the given step.
     */
    private static Map<UserOverride, String> getUserOverrides(NeoFormRuntimeSpecification spec, String stepName, UserOverride... supportedOverrides) {
        Project project = spec.getProject();
        Map<UserOverride, String> userOverrides = new EnumMap<>(UserOverride.class);

        for (UserOverride override : supportedOverrides) {
            Object genericOverride = project.findProperty("neoform.all." + override.property);
            if (genericOverride != null) {
                userOverrides.put(override, genericOverride.toString());
            }

            Object stepOverride = project.findProperty("neoform." + stepName + "." + override.property);
            if (stepOverride != null) {
                userOverrides.put(override, stepOverride.toString());
            }
        }

        return userOverrides;
    }

    private static void appendUserOverridesToDescription(Task task, Map<UserOverride, String> userOverrides) {
        if (!userOverrides.isEmpty()) {
            String descriptionText = "User overrides: " + userOverrides.entrySet().stream()
                    .map(uo -> uo.getKey().property + ": " + uo.getValue())
                    .collect(Collectors.joining(", "));

            if (task.getDescription() != null) {
                task.setDescription(task.getDescription() + " " + descriptionText);
            } else {
                task.setDescription(descriptionText);
            }
        }
    }

    enum UserOverride {
        VERSION("version"),
        MAX_MEMORY("maxMemory");

        private final String property;

        UserOverride(String property) {
            this.property = property;
        }
    }
}
