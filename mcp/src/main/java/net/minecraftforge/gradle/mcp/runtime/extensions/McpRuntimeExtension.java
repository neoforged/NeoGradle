package net.minecraftforge.gradle.mcp.runtime.extensions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.tasks.Execute;
import net.minecraftforge.gradle.common.runtime.tasks.ListLibraries;
import net.minecraftforge.gradle.common.util.FileUtils;
import net.minecraftforge.gradle.common.util.VersionJson;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.Artifact;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.dsl.common.util.NamingConstants;
import net.minecraftforge.gradle.dsl.mcp.configuration.McpConfigConfigurationSpecV1;
import net.minecraftforge.gradle.dsl.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.dsl.mcp.extensions.Mcp;
import net.minecraftforge.gradle.mcp.runtime.definition.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.specification.McpRuntimeSpecification;
import net.minecraftforge.gradle.mcp.runtime.tasks.InjectCode;
import net.minecraftforge.gradle.mcp.runtime.tasks.Patch;
import net.minecraftforge.gradle.mcp.runtime.tasks.RecompileSourceJar;
import net.minecraftforge.gradle.mcp.runtime.tasks.StripJar;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"}) // API Design
public abstract class McpRuntimeExtension extends CommonRuntimeExtension<McpRuntimeSpecification, McpRuntimeSpecification.Builder, McpRuntimeDefinition> {

    @javax.inject.Inject
    public McpRuntimeExtension(Project project) {
        super(project);

        this.getDefaultVersion().convention(
                project.provider(() -> project.getExtensions().getByType(Mcp.class))
                        .flatMap(Mcp::getMcpConfigArtifact)
                        .map(Artifact::getVersion)
        );
    }

    private static void configureMcpRuntimeTaskWithDefaults(McpRuntimeSpecification spec, File mcpDirectory, Map<String, File> data, LinkedHashMap<String, TaskProvider<? extends WithOutput>> tasks, McpConfigConfigurationSpecV1.Step step, Runtime mcpRuntimeTask, Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        mcpRuntimeTask.getArguments().set(buildArguments(spec, step, tasks, mcpRuntimeTask, alternativeInputProvider));
        configureCommonRuntimeTaskParameters(mcpRuntimeTask, data, step.getName(), spec, mcpDirectory);
    }

    private static void configureMcpRuntimeTaskWithDefaults(McpRuntimeSpecification spec, File mcpDirectory, Map<String, File> data, Runtime mcpRuntimeTask) {
        mcpRuntimeTask.getArguments().set(Maps.newHashMap());
        configureCommonRuntimeTaskParameters(mcpRuntimeTask, data, CommonRuntimeUtils.buildStepName(spec, mcpRuntimeTask.getName()), spec, mcpDirectory);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable
    private static TaskProvider<? extends WithOutput> createBuiltIn(final McpRuntimeSpecification spec, McpConfigConfigurationSpecV2 mcpConfigV2, McpConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTaskProviders, final Optional<TaskProvider<? extends WithOutput>> adaptedInput) {
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
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), StripJar.class, task -> task.getInput().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step)));
            case "listLibraries":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), ListLibraries.class, task -> {
                    task.getDownloadedVersionJsonFile().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                });
            case "inject":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), InjectCode.class, task -> {
                    task.getInjectionSource().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step));
                    if (spec.getDistribution().equals(DistributionType.SERVER)) {
                        task.getInclusionFilter().set("**/server/**");
                    } else if (spec.getDistribution().equals(DistributionType.CLIENT)) {
                        task.getInclusionFilter().set("**/client/**");
                    }
                });
            case "patch":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Patch.class, task -> task.getInput().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step)));
        }
        if (mcpConfigV2.getSpec() >= 2) {
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

    private static TaskProvider<? extends Runtime> createExecute(final McpRuntimeSpecification spec, final McpConfigConfigurationSpecV1.Step step, final McpConfigConfigurationSpecV1.Function function) {
        return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            task.getExecutingArtifact().set(function.getVersion());
            task.getJvmArguments().addAll(function.getJvmArgs());
            task.getProgramArguments().addAll(function.getArgs());
        });
    }

    private static Map<String, Provider<String>> buildArguments(final McpRuntimeSpecification spec, McpConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        final Map<String, Provider<String>> arguments = new HashMap<>();

        step.getValues().forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends WithOutput>> dependentTask;
                if (!Objects.equals(key, "input") || !alternativeInputProvider.isPresent()) {
                    dependentTask = McpRuntimeUtils.getInputTaskForTaskFrom(spec, value, tasks);
                } else {
                    dependentTask = alternativeInputProvider;
                }

                dependentTask.ifPresent(taskForArguments::dependsOn);
                dependentTask.ifPresent(task -> arguments.put(key, task.flatMap(t -> t.getOutput().getAsFile().map(File::getAbsolutePath))));
            } else {
                arguments.put(key, spec.getProject().provider(() -> value));
            }
        });

        return arguments;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Map<String, File> buildDataMap(McpConfigConfigurationSpecV2 mcpConfig, final DistributionType side, final File unpackedMcpDirectory) {
        return mcpConfig.getData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new File(unpackedMcpDirectory, e.getValue() instanceof Map ? ((Map<String, String>) e.getValue()).get(side.getName()) : (String) e.getValue())
        ));
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @NotNull
    protected McpRuntimeDefinition doCreate(final McpRuntimeSpecification spec) {
        if (this.runtimes.containsKey(spec.getName()))
            throw new IllegalArgumentException("Cannot register runtime with name '" + spec.getName() + "' because it already exists");

        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);
        final Dependency mcpConfigDependency = spec.getProject().getDependencies().create("de.oceanlabs.mcp:mcp_config:" + spec.getMcpVersion() + "@zip");
        final Configuration mcpDownloadConfiguration = spec.getProject().getConfigurations().detachedConfiguration(mcpConfigDependency);
        final ResolvedConfiguration resolvedConfiguration = mcpDownloadConfiguration.getResolvedConfiguration();
        final File mcpZipFile = resolvedConfiguration.getFiles().iterator().next();

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

        final File mcpDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("mcp/%s", spec.getName())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(mcpDirectory, "unpacked");
        final File stepsMcpDirectory = new File(mcpDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        FileUtils.unzip(mcpZipFile, unpackedMcpZipDirectory);

        final File mcpConfigFile = new File(unpackedMcpZipDirectory, "config.json");
        final McpConfigConfigurationSpecV2 mcpConfig = McpConfigConfigurationSpecV2.get(mcpConfigFile);

        mcpConfig.getLibraries(spec.getDistribution().getName()).forEach(library -> minecraftDependenciesConfiguration.getDependencies().add(
                spec.getProject().getDependencies().create(library)
        ));

        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec, mcpDirectory);

        final Map<String, File> data = buildDataMap(mcpConfig, spec.getDistribution(), unpackedMcpZipDirectory);

        final TaskProvider<? extends ArtifactProvider> sourceJarTask = spec.getProject().getTasks().register("supplySourcesFor" + spec.getName(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(mcpDirectory, "sources.jar"));
        });
        final TaskProvider<? extends ArtifactProvider> rawJarTask = spec.getProject().getTasks().register("supplyRawJarFor" + spec.getName(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(mcpDirectory, "raw.jar"));
        });

        return new McpRuntimeDefinition(spec, new LinkedHashMap<>(), sourceJarTask, rawJarTask, gameArtifactTasks, gameArtifacts, minecraftDependenciesConfiguration, taskProvider -> taskProvider.configure(runtimeTask -> {
            configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, runtimeTask);
        }), unpackedMcpZipDirectory, mcpConfig);
    }

    @Override
    protected McpRuntimeSpecification.Builder createBuilder() {
        return McpRuntimeSpecification.Builder.from(getProject());
    }

    @Override
    protected void bakeDefinition(McpRuntimeDefinition definition) {
        final McpRuntimeSpecification spec = definition.getSpecification();
        final McpConfigConfigurationSpecV2 mcpConfig = definition.getMcpConfig();

        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final File mcpDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("mcp/%s", spec.getName())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(mcpDirectory, "unpacked");
        final File stepsMcpDirectory = new File(mcpDirectory, "steps");

        final Map<String, String> versionData = Maps.newHashMap(mappingsExtension.getVersion().get());
        versionData.put(NamingConstants.Version.MINECRAFT_VERSION, spec.getMinecraftVersion());
        versionData.put(McpRuntimeConstants.Naming.Version.MCP_RUNTIME, spec.getName());
        definition.setMappingVersionData(versionData);

        final Map<String, File> data = buildDataMap(mcpConfig, spec.getDistribution(), unpackedMcpZipDirectory);

        final List<McpConfigConfigurationSpecV1.Step> steps = mcpConfig.getSteps(spec.getDistribution().getName());
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Unknown side: " + spec.getDistribution() + " For Config: " + definition.getSpecification().getMcpVersion());
        }

        final LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs = definition.getTasks();
        for (McpConfigConfigurationSpecV1.Step step : steps) {
            Optional<TaskProvider<? extends WithOutput>> adaptedInput = Optional.empty();

            if (spec.getPreTaskTypeAdapters().containsKey(step.getName())) {
                final String inputArgumentMarker = step.getValue("input");
                if (inputArgumentMarker == null) {
                    throw new IllegalStateException("Can not change input chain on: " + step.getName() + " it has no input to transform!");
                }

                Optional<TaskProvider<? extends WithOutput>> inputTask = McpRuntimeUtils.getInputTaskForTaskFrom(spec, inputArgumentMarker, taskOutputs);

                if (!spec.getPreTaskTypeAdapters().get(step.getName()).isEmpty() && inputTask.isPresent()) {
                    for (TaskTreeAdapter taskTreeAdapter : spec.getPreTaskTypeAdapters().get(step.getName())) {
                        final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(definition, inputTask.get(), mcpDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));
                        if (modifiedTree != null) {
                            modifiedTree.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task));
                            inputTask = Optional.of(modifiedTree);
                        }
                    }

                    adaptedInput = inputTask;
                }
            }

            TaskProvider<? extends WithOutput> mcpRuntimeTaskProvider = createBuiltIn(spec, mcpConfig, step, taskOutputs, definition.getGameArtifactProvidingTasks(), adaptedInput);

            if (mcpRuntimeTaskProvider == null) {
                McpConfigConfigurationSpecV1.Function function = mcpConfig.getFunction(step.getType());
                if (function == null) {
                    throw new IllegalArgumentException(String.format("Invalid MCP Config, Unknown function step type: %s File: %s", step.getType(), mcpConfig));
                }

                mcpRuntimeTaskProvider = createExecute(spec, step, function);
            }

            Optional<TaskProvider<? extends WithOutput>> finalAdaptedInput = adaptedInput;
            mcpRuntimeTaskProvider.configure((WithOutput mcpRuntimeTask) -> {
                if (mcpRuntimeTask instanceof Runtime) {
                    final Runtime runtimeTask = (Runtime) mcpRuntimeTask;
                    configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, taskOutputs, step, runtimeTask, finalAdaptedInput);
                }
            });

            if (!spec.getPostTypeAdapters().containsKey(step.getName())) {
                taskOutputs.put(mcpRuntimeTaskProvider.getName(), mcpRuntimeTaskProvider);
            } else {
                for (TaskTreeAdapter taskTreeAdapter : spec.getPostTypeAdapters().get(step.getName())) {
                    final TaskProvider<? extends Runtime> taskProvider = taskTreeAdapter.adapt(definition, mcpRuntimeTaskProvider, mcpDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), dependentTaskProvider -> dependentTaskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));
                    if (taskProvider != null) {
                        taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task));
                        mcpRuntimeTaskProvider = taskProvider;
                    }
                }

                taskOutputs.put(mcpRuntimeTaskProvider.getName(), mcpRuntimeTaskProvider);
            }
        }

        final TaskProvider<? extends WithOutput> lastTask = Iterators.getLast(taskOutputs.values().iterator());
        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                spec.getProject(), String.format("mapGameFor%s", StringUtils.capitalize(spec.getName())), taskName -> CommonRuntimeUtils.buildTaskName(spec, taskName), lastTask, definition.getGameArtifactProvidingTasks(), versionData, additionalRuntimeTasks, definition
        );

        final TaskProvider<? extends Runtime> remapTask = context.getNamingChannel().getApplySourceMappingsTaskBuilder().get().build(context);
        additionalRuntimeTasks.forEach(taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));
        remapTask.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task));

        final FileCollection recompileDependencies = spec.getAdditionalRecompileDependencies().plus(spec.getProject().files(definition.getMinecraftDependenciesConfiguration()));
        final TaskProvider<? extends Runtime> recompileTask = spec.getProject()
                .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, recompileSourceJar -> {
                    recompileSourceJar.getInputJar().set(remapTask.flatMap(WithOutput::getOutput));
                    recompileSourceJar.getCompileClasspath().setFrom(recompileDependencies);
                    recompileSourceJar.getStepName().set("recompile");
                });
        recompileTask.configure(mcpRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, mcpRuntimeTask));

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

    public abstract Property<DistributionType> getDefaultDistributionType();

    public abstract Property<String> getDefaultVersion();

}
