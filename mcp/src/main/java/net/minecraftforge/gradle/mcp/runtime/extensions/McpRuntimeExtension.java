package net.minecraftforge.gradle.mcp.runtime.extensions;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.util.Artifact;
import net.minecraftforge.gradle.common.util.FileUtils;
import net.minecraftforge.gradle.common.util.FileWrapper;
import net.minecraftforge.gradle.mcp.McpExtension;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV1;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.spec.builder.McpRuntimeSpecBuilder;
import net.minecraftforge.gradle.mcp.runtime.tasks.*;
import net.minecraftforge.gradle.mcp.util.CacheableMinecraftVersion;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
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
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class McpRuntimeExtension {
    private static final Pattern OUTPUT_REPLACE_PATTERN = Pattern.compile("^\\{(\\w+)Output}$");

    private final Project project;

    private final Map<String, McpRuntimeDefinition> runtimes = Maps.newHashMap();

    @javax.inject.Inject
    public McpRuntimeExtension(Project project) {
        this.project = project;

        this.getDefaultSide().convention("joined");
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

        return create(spec);
    }

    public McpRuntimeDefinition register(final Action<McpRuntimeSpecBuilder> configurator) {
       final McpRuntimeSpec spec = createSpec(configurator);

        if (this.runtimes.containsKey(spec.name())) {
            throw new IllegalArgumentException("A runtime with the name '" + spec.name() + "' already exists");
        }

        final McpRuntimeDefinition definition = create(spec);
        this.runtimes.put(spec.name(), definition);
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
        final Dependency mcpConfigDependency = spec.project().getDependencies().create("de.oceanlabs.mcp:mcp_config:" + spec.mcpVersion() + "@zip");
        final Configuration mcpDownloadConfiguration = spec.project().getConfigurations().detachedConfiguration(mcpConfigDependency);
        final ResolvedConfiguration resolvedConfiguration = mcpDownloadConfiguration.getResolvedConfiguration();
        final File mcpZipFile = resolvedConfiguration.getFiles().iterator().next();

        final File globalCache = new File(spec.project().getGradle().getGradleUserHomeDir(), "caches");
        final File mcpCache = new File(globalCache, "mcp");
        final File minecraftCache = new File(globalCache, "minecraft");

        final File mcpDirectory = spec.project().getLayout().getBuildDirectory().dir("mcp/%s".formatted(spec.name())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(mcpCache, "unpacked");
        final File stepsMcpDirectory = new File(mcpDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        FileUtils.unzip(mcpZipFile, unpackedMcpZipDirectory);

        final File mcpConfigFile = new File(unpackedMcpZipDirectory, "config.json");
        final McpConfigConfigurationSpecV2 mcpConfig = McpConfigConfigurationSpecV2.get(mcpConfigFile);

        final Map<String, FileWrapper> data = buildDataMap(mcpConfig, spec.side(), unpackedMcpZipDirectory);

        final List<McpConfigConfigurationSpecV1.Step> steps = mcpConfig.getSteps(spec.side());
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Unknown side: " + spec.side() + " For Config: " + mcpZipFile);
        }

        final LinkedHashMap<String, TaskProvider<? extends McpRuntime>> tasks = new LinkedHashMap<>();
        for (McpConfigConfigurationSpecV1.Step step : steps) {
            Optional<Provider<File>> adaptedInput = Optional.empty();

            if (step.getName().equals("decompile")) {
                final String inputArgumentMarker = step.getValue("input");
                final Provider<File> inputArtifact = getInputForTaskFrom(spec, inputArgumentMarker, tasks);

                if (spec.preDecompileTaskTreeModifier() != null) {
                    final McpRuntime modifiedTree = spec.preDecompileTaskTreeModifier().adapt(spec, inputArtifact);
                    adaptedInput = Optional.of(modifiedTree.getOutput().getAsFile());
                }
            }

            TaskProvider<? extends McpRuntime> mcpRuntimeTaskProvider = createBuiltIn(spec, mcpConfig, step, minecraftCache, tasks, adaptedInput);

            if (mcpRuntimeTaskProvider == null) {
                McpConfigConfigurationSpecV1.Function function = mcpConfig.getFunction(step.getType());
                if (function == null) {
                    throw new IllegalArgumentException("Invalid MCP Config, Unknown function step type: %s File: %s".formatted(step.getType(), mcpConfig));
                }

                mcpRuntimeTaskProvider = createExecute(spec, step, function);
            }

            tasks.put(mcpRuntimeTaskProvider.getName(), mcpRuntimeTaskProvider);
            mcpRuntimeTaskProvider.configure((McpRuntime mcpRuntimeTask) -> {
                mcpRuntimeTask.getArguments().set(buildArguments(spec, step, tasks, mcpRuntimeTask));
                mcpRuntimeTask.getData().set(data);
                mcpRuntimeTask.getStepName().set(step.getName());
                mcpRuntimeTask.getSide().set(spec.side());
                mcpRuntimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.minecraftVersion()));
                mcpRuntimeTask.getMcpDirectory().set(mcpDirectory);
                mcpRuntimeTask.getJavaVersion().convention(spec.configureProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
            });
        }

        return new McpRuntimeDefinition(spec, tasks, unpackedMcpZipDirectory, mcpConfig);
    }

    public TaskProvider<? extends AccessTransformer> createAt(McpRuntimeSpec runtimeSpec, List<File> files, Collection<String> data) {
        return getProject().getTasks().register(McpRuntimeUtils.buildTaskName(runtimeSpec, "accessTransformer"), AccessTransformer.class, task -> {
            task.getAdditionalTransformers().addAll(data);
            task.getTransformers().plus(project.files(files.toArray()));
        });
    }

    /**
     * Internal Use Only
     * Non-Public API, Can be changed at any time.
     */
    public TaskProvider<? extends SideAnnotationStripper> createSAS(McpRuntimeSpec spec, List<File> files, Collection<String> data) {
        return getProject().getTasks().register(McpRuntimeUtils.buildTaskName(spec, "sideAnnotationStripper"), SideAnnotationStripper.class, task -> {
            task.getAdditionalDataEntries().addAll(data);
            task.getDataFiles().plus(project.files(files.toArray()));
        });
    }

    public abstract Property<String> getDefaultSide();

    public abstract Property<String> getDefaultVersion();

    public final Provider<Map<String, McpRuntimeDefinition>> getRuntimes() {
        return getProject().provider(() -> this.runtimes);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable
    private static TaskProvider<? extends McpRuntime> createBuiltIn(final McpRuntimeSpec spec, McpConfigConfigurationSpecV2 mcpConfigV2, McpConfigConfigurationSpecV1.Step step, final File globalMinecraftCacheFile, final Map<String, TaskProvider<? extends McpRuntime>> tasks, final Optional<Provider<File>> adaptedInput) {
        switch (step.getType()) {
            case "downloadManifest":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), FileCacheProviding.class, task -> {
                    task.getOutputFileName().set("manifest.json");
                    task.getFileCache().set(globalMinecraftCacheFile);
                    task.getSelector().set(FileCacheProviding.ISelector.launcher());
                    task.setDescription("Provides the Minecraft Launcher Manifest from the global Minecraft File Cache");
                });
            case "downloadJson":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), FileCacheProviding.class, task -> {
                    task.getOutputFileName().set("%s.json".formatted(spec.minecraftVersion()));
                    task.getFileCache().set(globalMinecraftCacheFile);
                    task.getSelector().set(FileCacheProviding.ISelector.forVersion(spec.minecraftVersion()));
                    task.setDescription("Provides the Minecraft Version JSON from the global Minecraft File Cache");
                });
            case "downloadClient":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), DownloadCore.class, task -> {
                    task.getDownloadedVersionJson().fileProvider(getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                    task.getArtifact().set("client");
                    task.getExtension().set("jar");
                });
            case "downloadServer":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), DownloadCore.class, task -> {
                    task.getDownloadedVersionJson().fileProvider(getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                    task.getArtifact().set("server");
                    task.getExtension().set("jar");
                });
            case "strip":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), StripJar.class, task -> {
                    task.getInput().fileProvider(getTaskInputFor(spec, tasks, step));
                });
            case "listLibraries":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), ListLibraries.class, task -> {
                    if (tasks.containsKey(McpRuntimeUtils.buildTaskName(spec, "downloadServer"))) {
                        task.getServerBundleFile().fileProvider(getTaskInputFor(spec, tasks, step, "downloadServer", adaptedInput));
                    }

                    task.getDownloadedVersionJsonFile().fileProvider(getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                });
            case "inject":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), Inject.class, task -> {
                    task.getInjectionSource().fileProvider(getTaskInputFor(spec, tasks, step));
                });
            case "patch":
                return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), Patch.class, task -> {
                    task.getInput().fileProvider(getTaskInputFor(spec, tasks, step));
                });
        }
        if (mcpConfigV2.getSpec() >= 2) {
            switch (step.getType()) {
                case "downloadClientMappings":
                    return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), DownloadCore.class, task -> {
                        task.getDownloadedVersionJson().fileProvider(getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                        task.getArtifact().set("client_mappings");
                        task.getExtension().set("txt");
                    });
                case "downloadServerMappings":
                    return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), DownloadCore.class, task -> {
                        task.getDownloadedVersionJson().fileProvider(getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                        task.getArtifact().set("server_mappings");
                        task.getExtension().set("txt");
                    });
            }
        }

        return null;
    }

    private static TaskProvider<? extends McpRuntime> createExecute(final McpRuntimeSpec spec, final McpConfigConfigurationSpecV1.Step step, final McpConfigConfigurationSpecV1.Function function) {
        return spec.project().getTasks().register(McpRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            task.getExecutingArtifact().set(function.getVersion());
            task.getJvmArguments().addAll(function.getJvmArgs());
            task.getProgramArguments().addAll(function.getArgs());
        });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Provider<File> getTaskInputFor(final McpRuntimeSpec spec, final Map<String, TaskProvider<? extends McpRuntime>> tasks, McpConfigConfigurationSpecV1.Step step, final String defaultInputTask, final Optional<Provider<File>> adaptedInput) {
        if (adaptedInput.isPresent()) {
            return adaptedInput.get();
        }

        if (step.getValue("input") == null) {
            return getInputForTaskFrom(spec, "{" + defaultInputTask + "Output}", tasks);
        }

        return getInputForTaskFrom(spec, step.getValue("input"), tasks);
    }

    private static Provider<File> getTaskInputFor(final McpRuntimeSpec spec, final Map<String, TaskProvider<? extends McpRuntime>> tasks, McpConfigConfigurationSpecV1.Step step) {
        return getInputForTaskFrom(spec, step.getValue("input"), tasks);
    }

    private static Provider<File> getInputForTaskFrom(final McpRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends McpRuntime>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return spec.project().provider(() -> new File(inputValue));
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            return tasks.computeIfAbsent(McpRuntimeUtils.buildTaskName(spec, stepName), value -> {
                throw new IllegalArgumentException("Could not find mcp task for input: " + value);
            }).flatMap(t -> t.getOutput().getAsFile());
        }

        throw new IllegalStateException("The string '" + inputValue + "' did not return a valid substitution match!");
    }

    public static Optional<TaskProvider<? extends McpRuntime>> getInputTaskForTaskFrom(final McpRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends McpRuntime>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            return Optional.ofNullable(tasks.get(McpRuntimeUtils.buildTaskName(spec, stepName)));
        }

        return Optional.empty();
    }

    private static Map<String, String> buildArguments(final McpRuntimeSpec spec, McpConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends McpRuntime>> tasks, final McpRuntime taskForArguments) {
        final Map<String, String> arguments = new HashMap<>();

        step.getValues().forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                final Optional<TaskProvider<? extends McpRuntime>> dependentTask = getInputTaskForTaskFrom(spec, value, tasks);
                dependentTask.ifPresent(taskForArguments::dependsOn);
                arguments.put(key, getInputForTaskFrom(spec, value, tasks).get().getAbsolutePath());
            } else {
                arguments.put(key, value);
            }
        });

        return arguments;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Map<String, FileWrapper> buildDataMap(McpConfigConfigurationSpecV2 mcpConfig, final String side, final File unpackedMcpDirectory) {
        return mcpConfig.getData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new FileWrapper(new File(unpackedMcpDirectory, e.getValue() instanceof Map ? ((Map<String, String>) e.getValue()).get(side) : (String) e.getValue()))
        ));
    }

}
