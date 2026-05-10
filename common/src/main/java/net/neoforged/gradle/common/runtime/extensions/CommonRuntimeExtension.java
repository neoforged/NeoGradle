package net.neoforged.gradle.common.runtime.extensions;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.runtime.tasks.InProcessSourceJarRecompiler;
import net.neoforged.gradle.common.runtime.tasks.RecompileSourceJar;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Recompiler;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runtime.extensions.CommonRuntimes;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.runtime.tasks.AsPartOfStep;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.process.CommandLineArgumentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class CommonRuntimeExtension<S extends CommonRuntimeSpecification, B extends CommonRuntimeSpecification.Builder<S, B>, D extends CommonRuntimeDefinition<S>> implements CommonRuntimes<S, B, D> {
    public record CustomCompilerArgsProvider(Provider<List<String>> args) implements CommandLineArgumentProvider
    {
        @Override
        public Iterable<String> asArguments() {
            return args.get();
        }
    }

    protected final Map<String, D>          definitions  = Maps.newHashMap();
    protected final Map<String, Dependency> dependencies = Maps.newHashMap();
    private final   Project                 project;

    protected CommonRuntimeExtension(Project project) {
        this.project = project;
        this.project.getExtensions().getByType(RuntimesExtension.class).add(this);
    }

    public static void configureCommonRuntimeTaskParameters(Runtime runtimeTask, Map<String, String> symbolicDataSources, String step, Specification spec, File runtimeDirectory) {
        runtimeTask.getSymbolicDataSources().set(symbolicDataSources);
        runtimeTask.getStepName().set(step);
        runtimeTask.getDistribution().set(spec.getDistribution());
        runtimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.getMinecraftVersion(), spec.getProject()).getFull());
        runtimeTask.getRuntimeDirectory().set(runtimeDirectory);
        runtimeTask.getRuntimeName().set(spec.getVersionedName());
        runtimeTask.getJavaVersion().convention(spec.getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());

        for (TaskCustomizer<? extends Task> taskCustomizer : spec.getTaskCustomizers().get(step)) {
            taskCustomizer.apply(runtimeTask);
        }
    }

    public static <T extends WithOutput & Task & AsPartOfStep> void configureOutputForSimpleStepTaskParameters(T runtimeTask, String step, Specification spec, File runtimeDirectory) {
        runtimeTask.getStepName().set(step);
        runtimeTask.getRuntimeDirectory().set(runtimeDirectory);

        for (TaskCustomizer<?> taskCustomizer : spec.getTaskCustomizers().get(step)) {
            taskCustomizer.apply(runtimeTask);
        }
    }

    public static void configureCommonRuntimeTaskParameters(Runtime runtime, CommonRuntimeDefinition<?> runtimeDefinition, File workingDirectory) {
        configureCommonRuntimeTaskParameters(runtime, runtimeDefinition.getSpecification(), workingDirectory);
        runtime.getJavaVersion().set(runtimeDefinition.getRequiredJavaVersion());

    }

    public static void configureCommonRuntimeTaskParameters(Runtime runtime, String stepName, CommonRuntimeSpecification specification, File workingDirectory) {
        configureCommonRuntimeTaskParameters(runtime, Collections.emptyMap(), stepName, specification, workingDirectory);
    }

    private static void configureCommonRuntimeTaskParameters(Runtime runtime, CommonRuntimeSpecification specification, File workingDirectory) {
        configureCommonRuntimeTaskParameters(runtime, runtime.getName(), specification, workingDirectory);
    }

    public static Map<GameArtifact, TaskProvider<? extends WithOutput>> buildDefaultArtifactProviderTasks(final Specification spec) {
        final MinecraftArtifactCache artifactCache = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);
        return artifactCache.cacheGameVersionTasks(spec.getProject(), spec.getMinecraftVersion(), spec.getDistribution());
    }

    public static Configuration extractVersionJsonLibraries(final @NotNull Project project, final String minecraftVersion, final CommonRuntimeDefinition<?> definition)
    {
        return ConfigurationUtils.temporaryConfiguration(
            project,
            "%sDependencies".formatted(minecraftVersion),
            files -> files.getDependencies().addAllLater(
                definition.getVersionJson()
                    .map(VersionJson::getLibraries)
                    .map(libraries -> libraries.stream().filter(VersionJson.Library::isAllowed).toList())
                    .map(libraries -> libraries.stream()
                        .map(VersionJson.Library::getName)
                        .map(project.getDependencies()::create)
                        .toList()
                    )
            )
        );
    }

    protected static @NotNull TaskProvider<? extends Runtime> createRecompileTask(
        final CommonRuntimeDefinition<?> definition,
        final TaskProvider<? extends WithOutput> recompileInput,
        final FileCollection recompileDependencies,
        final Consumer<TaskProvider<? extends Runtime>> configure)
    {
        //TODO: Future change can add the new InProcess Recompiler.
        return createGradleRecompileTask(definition, recompileInput, recompileDependencies, configure);
    }

    private static @NotNull TaskProvider<InProcessSourceJarRecompiler> createNativeRecompileTask(
        final CommonRuntimeDefinition<?> definition,
        final TaskProvider<? extends WithOutput> recompileInput,
        final FileCollection recompileDependencies,
        final Consumer<TaskProvider<? extends Runtime>> configure)
    {
        final CommonRuntimeSpecification spec = definition.getSpecification();
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

    private static @NotNull TaskProvider<RecompileSourceJar> createGradleRecompileTask(
        final CommonRuntimeDefinition<?> definition,
        final TaskProvider<? extends WithOutput> recompileInput,
        final FileCollection recompileDependencies,
        final Consumer<TaskProvider<? extends Runtime>> configure)
    {
        final Provider<? extends FileTree>  recompileSourceFileTree = recompileInput.flatMap(WithOutput::getOutputAsTree);

        final CommonRuntimeSpecification spec = definition.getSpecification();
        // Consider user-settings
        final TaskProvider<RecompileSourceJar> recompileSourceJar = spec.getProject()
                .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, task -> {
                    task.getCompileFileRoot().from(recompileSourceFileTree);
                    task.getAdditionalInputFileRoot().from(definition.getAdditionalCompileSources());
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

    @Override
    public Project getProject() {
        return project;
    }
    
    @Override
    public final Map<String, D> getDefinitions() {
        return this.definitions;
    }

    @Deprecated
    public final Provider<Map<String, D>> getRuntimes() {
        return project.provider(this::getDefinitions);
    }

    @Override
    @NotNull
    public final D maybeCreate(final Action<B> configurator) {
        final S spec = createSpec(configurator);
        if (definitions.containsKey(spec.getIdentifier()))
            return definitions.get(spec.getIdentifier());

        return create(configurator);
    }

    @Override
    public D maybeCreateFor(Dependency dependency, Action<B> configurator) {
        final D result = maybeCreate(configurator);
        dependencies.put(result.getSpecification().getIdentifier(), dependency);
        return result;
    }

    @Override
    @NotNull
    public final D create(final Action<B> configurator) {
        final S spec = createSpec(configurator);

        if (project.getExtensions().getByType(RuntimesExtension.class).definitionExists(spec.getIdentifier()))
            throw new IllegalArgumentException(String.format("Runtime with identifier '%s' already exists", spec.getIdentifier()));

        final D runtime = doCreate(spec);
        definitions.put(spec.getIdentifier(), runtime);
        afterRegistration(runtime);
        return runtime;
    }

    protected void afterRegistration(D runtime) {
        // no-op
    }

    @Override
    public D create(Dependency dependency, Action<B> configurator) {
        final D result = create(configurator);
        dependencies.put(result.getSpecification().getIdentifier(), dependency);
        return result;
    }

    @NotNull
    protected abstract D doCreate(final S spec);

    @NotNull
    private S createSpec(Action<B> configurator) {
        final B builder = createBuilder();
        configurator.execute(builder);
        return builder.build();
    }

    @Override
    @NotNull
    public final D getByName(final String name) {
        return this.definitions.computeIfAbsent(name, (n) -> {
            throw new RuntimeException(String.format("Failed to find runtime with name: %s", n));
        });
    }

    @Override
    @Nullable
    public final D findByNameOrIdentifier(final String name) {
        final D byIdentifier = this.definitions.get(name);
        if (byIdentifier != null)
            return byIdentifier;

        return definitions.values().stream().filter(r -> r.getSpecification().getVersionedName().equals(name)).findAny().orElse(null);
    }

    protected abstract B createBuilder();

    @Override
    @NotNull
    public Set<D> findIn(final Configuration configuration) {

        final Repository repository = project.getExtensions().getByType(Repository.class);

        final Set<D> directDependency = configuration.getAllDependencies().
                stream().flatMap(dep -> getDefinitions().values().stream()
                        .filter(runtime -> dependencies.containsKey(runtime.getSpecification().getIdentifier()))
                        .filter(runtime -> repository.getEntries().stream().anyMatch(entry -> entry.getOriginal().equals(
                                dependencies.get(runtime.getSpecification().getIdentifier())
                        ))))
                .collect(Collectors.toSet());

        if (!directDependency.isEmpty())
            return directDependency;

        return project.getConfigurations().stream()
                .filter(config -> config.getHierarchy().contains(configuration))
                .flatMap(config -> config.getAllDependencies().stream())
                .flatMap(dep -> getDefinitions().values().stream()
                        .filter(runtime -> dependencies.containsKey(runtime.getSpecification().getIdentifier()))
                        .filter(runtime -> repository.getEntries().stream().anyMatch(entry -> entry.getOriginal().equals(
                                dependencies.get(runtime.getSpecification().getIdentifier())
                        ))))
                .collect(Collectors.toSet());
    }

    protected final TaskProvider<DownloadAssets> createDownloadAssetsTasks(final CommonRuntimeSpecification specification, final Provider<VersionJson> versionJson) {
        return specification.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(specification, "downloadAssets"), DownloadAssets.class, task -> {
            task.getVersionJson().set(versionJson);
        });
    }

    protected final TaskProvider<ExtractNatives> createExtractNativesTasks(final CommonRuntimeSpecification specification, final File runtimeDirectory, final Provider<VersionJson> versionJson) {
        return specification.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(specification, "extractNatives"), ExtractNatives.class, task -> {
            task.getVersionJson().set(versionJson);

            configureOutputForSimpleStepTaskParameters(task, "extractNatives", specification, runtimeDirectory);
            task.getOutputDirectory().set(task.getStepsDirectory().map(dir -> dir.dir("extractNatives")));
        });
    }
}
