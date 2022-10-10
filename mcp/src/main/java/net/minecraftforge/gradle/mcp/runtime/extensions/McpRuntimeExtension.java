package net.minecraftforge.gradle.mcp.runtime.extensions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.extensions.MinecraftArtifactCacheExtension;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.*;
import net.minecraftforge.gradle.mcp.McpExtension;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV1;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.extensions.MappingsExtension;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.naming.RenamingTaskBuildingContext;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.spec.builder.McpRuntimeSpecBuilder;
import net.minecraftforge.gradle.mcp.runtime.tasks.*;
import net.minecraftforge.gradle.mcp.util.CacheableMinecraftVersion;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static net.minecraftforge.gradle.common.util.GameArtifactUtils.doWhenRequired;

public abstract class McpRuntimeExtension {

    private final Project project;

    private final Map<String, McpRuntimeDefinition> runtimes = Maps.newHashMap();

    @javax.inject.Inject
    public McpRuntimeExtension(Project project) {
        this.project = project;

        this.getDefaultSide().convention(ArtifactSide.JOINED);
        this.getDefaultVersion().convention(
                project.provider(() -> project.getExtensions().getByType(McpExtension.class))
                        .flatMap(McpExtension::getMcpConfigArtifact)
                        .map(Artifact::getVersion)
        );
    }

    public Project getProject() {
        return project;
    }

    public McpRuntimeDefinition registerOrGet(final Action<McpRuntimeSpecBuilder> configurator) {
        final McpRuntimeSpec spec = createSpec(configurator);

        if (runtimes.containsKey(spec.name())) {
            final McpRuntimeDefinition other = runtimes.get(spec.name());
            if (!other.spec().equals(spec)) {
                throw new IllegalArgumentException("Cannot register runtime with name '" + spec.name() + "' because it already exists with a different spec");
            }
            return other;
        }

        final McpRuntimeDefinition definition = create(spec);
        runtimes.put(spec.name(), definition);
        return definition;
    }

    @NotNull
    private McpRuntimeSpec createSpec(Action<McpRuntimeSpecBuilder> configurator) {
        final McpRuntimeSpecBuilder builder = McpRuntimeSpecBuilder.from(project);
        configurator.execute(builder);
        return builder.build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NotNull
    private static McpRuntimeDefinition create(McpRuntimeSpec spec) {
        final McpMinecraftExtension mcpMinecraftExtension = spec.project().getExtensions().getByType(McpMinecraftExtension.class);
        final MappingsExtension mappingsExtension = mcpMinecraftExtension.getMappings();
        final MinecraftArtifactCacheExtension artifactCacheExtension = spec.project().getExtensions().getByType(MinecraftArtifactCacheExtension.class);
        final Dependency mcpConfigDependency = spec.project().getDependencies().create("de.oceanlabs.mcp:mcp_config:" + spec.mcpVersion() + "@zip");
        final Configuration mcpDownloadConfiguration = spec.project().getConfigurations().detachedConfiguration(mcpConfigDependency);
        final ResolvedConfiguration resolvedConfiguration = mcpDownloadConfiguration.getResolvedConfiguration();
        final File mcpZipFile = resolvedConfiguration.getFiles().iterator().next();

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final Map<GameArtifact, File> gameArtifacts = artifactCacheExtension.cacheGameVersion(spec.minecraftVersion(), spec.side());

        final VersionJson versionJson;
        try {
            versionJson = VersionJson.get(gameArtifacts.get(GameArtifact.VERSION_MANIFEST));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to read VersionJson from the launcher metadata for the minecraft version: %s".formatted(spec.minecraftVersion()), e);
        }

        final Configuration minecraftDependenciesConfiguration = spec.project().getConfigurations().detachedConfiguration();
        minecraftDependenciesConfiguration.setCanBeResolved(true);
        minecraftDependenciesConfiguration.setCanBeConsumed(false);
        for (VersionJson.Library library : versionJson.getLibraries()) {
            minecraftDependenciesConfiguration.getDependencies().add(
                    spec.project().getDependencies().create(library.getName())
            );
        }

        final File mcpDirectory = spec.project().getLayout().getBuildDirectory().dir("mcp/%s".formatted(spec.name())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(mcpDirectory, "unpacked");
        final File stepsMcpDirectory = new File(mcpDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        FileUtils.unzip(mcpZipFile, unpackedMcpZipDirectory);

        final File mcpConfigFile = new File(unpackedMcpZipDirectory, "config.json");
        final McpConfigConfigurationSpecV2 mcpConfig = McpConfigConfigurationSpecV2.get(mcpConfigFile);

        mcpConfig.getLibraries(spec.side().getName()).forEach(library -> {
            minecraftDependenciesConfiguration.getDependencies().add(
                    spec.project().getDependencies().create(library)
            );
        });

        final Map<String, File> data = buildDataMap(mcpConfig, spec.side(), unpackedMcpZipDirectory);

        final Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec, minecraftCache, mcpDirectory, data, spec.side());

        final List<McpConfigConfigurationSpecV1.Step> steps = mcpConfig.getSteps(spec.side().getName());
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Unknown side: " + spec.side() + " For Config: " + mcpZipFile);
        }

        final LinkedHashMap<String, TaskProvider<? extends IMcpRuntimeTask>> tasks = new LinkedHashMap<>();
        for (McpConfigConfigurationSpecV1.Step step : steps) {
            Optional<Provider<File>> adaptedInput = Optional.empty();

            if (step.getName().equals("decompile")) {
                final String inputArgumentMarker = step.getValue("input");
                final Provider<File> inputArtifact = McpRuntimeUtils.getInputForTaskFrom(spec, inputArgumentMarker, tasks);

                if (spec.preDecompileTaskTreeModifier() != null) {
                    final IMcpRuntimeTask modifiedTree = spec.preDecompileTaskTreeModifier().adapt(spec, inputArtifact);
                    adaptedInput = Optional.of(modifiedTree.getOutput().getAsFile());
                }
            }

            TaskProvider<? extends IMcpRuntimeTask> mcpRuntimeTaskProvider = createBuiltIn(spec, mcpConfig, step, minecraftCache, tasks, gameArtifactTasks, adaptedInput);

            if (mcpRuntimeTaskProvider == null) {
                McpConfigConfigurationSpecV1.Function function = mcpConfig.getFunction(step.getType());
                if (function == null) {
                    throw new IllegalArgumentException("Invalid MCP Config, Unknown function step type: %s File: %s".formatted(step.getType(), mcpConfig));
                }

                mcpRuntimeTaskProvider = createExecute(spec, step, function);
            }

            tasks.put(mcpRuntimeTaskProvider.getName(), mcpRuntimeTaskProvider);
            mcpRuntimeTaskProvider.configure((IMcpRuntimeTask mcpRuntimeTask) -> {
                configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, tasks, step, mcpRuntimeTask);
            });
        }

        final TaskProvider<? extends IMcpRuntimeTask> lastTask = Iterators.getLast(tasks.values().iterator());

        final Map<String, String> versionData = Maps.newHashMap(mappingsExtension.getMappingVersion().get());
        versionData.put(McpRuntimeConstants.Naming.Version.MINECRAFT_VERSION, spec.minecraftVersion());
        versionData.put(McpRuntimeConstants.Naming.Version.MCP_RUNTIME, spec.name());
        final RenamingTaskBuildingContext context = new RenamingTaskBuildingContext(
                spec, minecraftCache, tasks, mappingsExtension.getMappingChannel().get(), versionData, lastTask,
                unpackedMcpZipDirectory, mcpConfig, gameArtifacts, gameArtifactTasks
        );

        final TaskProvider<? extends IMcpRuntimeTask> remapTask = mappingsExtension.getMappingChannel()
                .get().getApplySourceMappingsTaskBuilder().get()
                .build(context);
        remapTask.configure(mcpRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, mcpRuntimeTask));
        context.additionalRuntimeTasks().forEach(taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));

        tasks.put(remapTask.getName(), remapTask);
        final TaskProvider<? extends IMcpRuntimeTask> recompileTask = spec.project()
                .getTasks().register(McpRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, recompileSourceJar -> {
                    recompileSourceJar.getInputJar().set(remapTask.flatMap(ITaskWithOutput::getOutput));
                    recompileSourceJar.getCompileClasspath().setFrom(minecraftDependenciesConfiguration);
                    recompileSourceJar.getStepName().set("recompile");
                });
        recompileTask.configure(mcpRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, mcpRuntimeTask));

        tasks.put(recompileTask.getName(), recompileTask);

        return new McpRuntimeDefinition(spec, tasks, unpackedMcpZipDirectory, mcpConfig, remapTask, recompileTask, gameArtifactTasks, minecraftDependenciesConfiguration);
    }

    private static void configureMcpRuntimeTaskWithDefaults(McpRuntimeSpec spec, File mcpDirectory, Map<String, File> data, LinkedHashMap<String, TaskProvider<? extends IMcpRuntimeTask>> tasks, McpConfigConfigurationSpecV1.Step step, IMcpRuntimeTask mcpRuntimeTask) {
        mcpRuntimeTask.getArguments().set(buildArguments(spec, step, tasks, mcpRuntimeTask));
        mcpRuntimeTask.getData().set(data);
        mcpRuntimeTask.getStepName().set(step.getName());
        mcpRuntimeTask.getDistribution().set(spec.side());
        mcpRuntimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.minecraftVersion()));
        mcpRuntimeTask.getMcpDirectory().set(mcpDirectory);
        mcpRuntimeTask.getJavaRuntimeVersion().convention(spec.configureProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    private static void configureMcpRuntimeTaskWithDefaults(McpRuntimeSpec spec, File mcpDirectory, Map<String, File> data, IMcpRuntimeTask mcpRuntimeTask) {
        mcpRuntimeTask.getArguments().set(Maps.newHashMap());
        mcpRuntimeTask.getData().set(data);
        mcpRuntimeTask.getStepName().set(mcpRuntimeTask.getName());
        mcpRuntimeTask.getDistribution().set(spec.side());
        mcpRuntimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.minecraftVersion()));
        mcpRuntimeTask.getMcpDirectory().set(mcpDirectory);
        mcpRuntimeTask.getJavaRuntimeVersion().convention(spec.configureProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    private static void configureGameArtifactProvidingTaskWithDefaults(McpRuntimeSpec spec, File mcpDirectory, Map<String, File> data, IMcpRuntimeTask mcpRuntimeTask, GameArtifact gameArtifact) {
        mcpRuntimeTask.getArguments().set(Maps.newHashMap());
        mcpRuntimeTask.getData().set(data);
        mcpRuntimeTask.getStepName().set("provide%s".formatted(StringUtils.capitalize(gameArtifact.name())));
        mcpRuntimeTask.getDistribution().set(spec.side());
        mcpRuntimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.minecraftVersion()));
        mcpRuntimeTask.getMcpDirectory().set(mcpDirectory);
        mcpRuntimeTask.getJavaRuntimeVersion().convention(spec.configureProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    public abstract Property<ArtifactSide> getDefaultSide();

    public abstract Property<String> getDefaultVersion();

    public final Provider<Map<String, McpRuntimeDefinition>> getRuntimes() {
        return getProject().provider(() -> this.runtimes);
    }

    private static Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> buildDefaultArtifactProviderTasks(final McpRuntimeSpec spec, final File minecraftCacheFile, final File mcpDirectory, final Map<String, File> data, final ArtifactSide side) {
        final EnumMap<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> result = Maps.newEnumMap(GameArtifact.class);

        doWhenRequired(GameArtifact.LAUNCHER_MANIFEST, side, () -> result.put(GameArtifact.LAUNCHER_MANIFEST, McpRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadManifest", "manifest.json", minecraftCacheFile, ICacheFileSelector.launcherMetadata(), "Provides the Minecraft Launcher Manifest from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.VERSION_MANIFEST, side, () -> result.put(GameArtifact.VERSION_MANIFEST, McpRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadJson", "%s.json".formatted(spec.minecraftVersion()), minecraftCacheFile, ICacheFileSelector.forVersionJson(spec.minecraftVersion()), "Provides the Minecraft Launcher Version Metadata from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.CLIENT_JAR, side, () -> result.put(GameArtifact.CLIENT_JAR, McpRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadClient", "client.jar", minecraftCacheFile, ICacheFileSelector.forVersionJar(spec.minecraftVersion(), "client"), "Provides the Minecraft Client Jar from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.SERVER_JAR, side, () -> result.put(GameArtifact.SERVER_JAR, McpRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadServer", "server.jar", minecraftCacheFile, ICacheFileSelector.forVersionJar(spec.minecraftVersion(), "server"), "Provides the Minecraft Server Jar from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.CLIENT_MAPPINGS, side, () -> result.put(GameArtifact.CLIENT_MAPPINGS, McpRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadClientMappings", "client_mappings.txt", minecraftCacheFile, ICacheFileSelector.forVersionMappings(spec.minecraftVersion(), "client"), "Provides the Minecraft Client Mappings from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.SERVER_MAPPINGS, side, () -> result.put(GameArtifact.SERVER_MAPPINGS, McpRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadServerMappings", "server_mappings.txt", minecraftCacheFile, ICacheFileSelector.forVersionMappings(spec.minecraftVersion(), "server"), "Provides the Minecraft Server Mappings from the global Minecraft File Cache")));

        result.forEach(((artifact, taskProvider) -> taskProvider.configure(task -> configureGameArtifactProvidingTaskWithDefaults(spec, mcpDirectory, data, task, artifact))));

        return result;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable
    private static TaskProvider<? extends IMcpRuntimeTask> createBuiltIn(final McpRuntimeSpec spec, McpConfigConfigurationSpecV2 mcpConfigV2, McpConfigConfigurationSpecV1.Step step, final File minecraftCacheFile, final Map<String, TaskProvider<? extends IMcpRuntimeTask>> tasks, final Map<GameArtifact, TaskProvider<? extends IMcpRuntimeTask>> gameArtifactTaskProviders, final Optional<Provider<File>> adaptedInput) {
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
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), StripJar.class, task -> {
                    task.getInput().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step));
                });
            case "listLibraries":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), ListLibraries.class, task -> {
                    if (tasks.containsKey(McpRuntimeUtils.buildTaskName(spec, "downloadServer"))) {
                        task.getServerBundleFile().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step, "downloadServer", adaptedInput));
                    }

                    task.getDownloadedVersionJsonFile().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                });
            case "inject":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), Inject.class, task -> {
                    task.getInjectionSource().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step));
                });
            case "patch":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), Patch.class, task -> {
                    task.getInput().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step));
                });
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

    private static TaskProvider<? extends IMcpRuntimeTask> createExecute(final McpRuntimeSpec spec, final McpConfigConfigurationSpecV1.Step step, final McpConfigConfigurationSpecV1.Function function) {
        return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            task.getExecutingArtifact().set(function.getVersion());
            task.getJvmArguments().addAll(function.getJvmArgs());
            task.getProgramArguments().addAll(function.getArgs());
        });
    }

    private static Map<String, String> buildArguments(final McpRuntimeSpec spec, McpConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends IMcpRuntimeTask>> tasks, final IMcpRuntimeTask taskForArguments) {
        final Map<String, String> arguments = new HashMap<>();

        step.getValues().forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                final Optional<TaskProvider<? extends IMcpRuntimeTask>> dependentTask = McpRuntimeUtils.getInputTaskForTaskFrom(spec, value, tasks);
                dependentTask.ifPresent(taskForArguments::dependsOn);
                arguments.put(key, McpRuntimeUtils.getInputForTaskFrom(spec, value, tasks).get().getAbsolutePath());
            } else {
                arguments.put(key, value);
            }
        });

        return arguments;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Map<String, File> buildDataMap(McpConfigConfigurationSpecV2 mcpConfig, final ArtifactSide side, final File unpackedMcpDirectory) {
        return mcpConfig.getData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new File(unpackedMcpDirectory, e.getValue() instanceof Map ? ((Map<String, String>) e.getValue()).get(side.getName()) : (String) e.getValue())
        ));
    }

}
