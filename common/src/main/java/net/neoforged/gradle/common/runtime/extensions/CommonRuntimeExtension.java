package net.neoforged.gradle.common.runtime.extensions;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.extensions.CommonRuntimes;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.GradleInternalUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class CommonRuntimeExtension<S extends CommonRuntimeSpecification, B extends CommonRuntimeSpecification.Builder<S, B>, D extends CommonRuntimeDefinition<S>> implements CommonRuntimes<S, B, D> {
    protected final Map<String, D> runtimes = Maps.newHashMap();
    private final Project project;

    protected CommonRuntimeExtension(Project project) {
        this.project = project;

        this.getDefaultDistributionType().convention(DistributionType.JOINED);
    }

    public static void configureCommonRuntimeTaskParameters(Runtime runtimeTask, Map<String, File> data, String step, Specification spec, File runtimeDirectory) {
        runtimeTask.getData().set(data);
        runtimeTask.getStepName().set(step);
        runtimeTask.getDistribution().set(spec.getDistribution());
        runtimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.getMinecraftVersion()));
        runtimeTask.getRuntimeDirectory().set(runtimeDirectory);
        runtimeTask.getJavaVersion().convention(spec.getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    public static void configureCommonRuntimeTaskParameters(Runtime mcpRuntimeTask, Map<String, File> data, String step, DistributionType distributionType, String minecraftVersion, Project project, File runtimeDirectory) {
        mcpRuntimeTask.getData().set(data);
        mcpRuntimeTask.getStepName().set(step);
        mcpRuntimeTask.getDistribution().set(distributionType);
        mcpRuntimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(minecraftVersion));
        mcpRuntimeTask.getRuntimeDirectory().set(runtimeDirectory);
        mcpRuntimeTask.getJavaVersion().convention(project.getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    public static Map<GameArtifact, TaskProvider<? extends WithOutput>> buildDefaultArtifactProviderTasks(final Specification spec) {
        final MinecraftArtifactCache artifactCache = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);
        return artifactCache.cacheGameVersionTasks(spec.getProject(), spec.getMinecraftVersion(), spec.getDistribution());
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public final Provider<Map<String, D>> getRuntimes() {
        return getProject().provider(() -> this.runtimes);
    }

    @Override
    @NotNull
    public final D maybeCreate(final Action<B> configurator) {
        final S spec = createSpec(configurator);
        if (runtimes.containsKey(spec.getName()))
            return runtimes.get(spec.getName());

        return create(configurator);
    }

    @Override
    @NotNull
    public final D create(final Action<B> configurator) {
        final S spec = createSpec(configurator);

        if (GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .anyMatch(ext -> ext.runtimes.containsKey(spec.getName())))
            throw new IllegalArgumentException(String.format("Runtime with name '%s' already exists", spec.getName()));

        final D runtime = doCreate(spec);
        runtimes.put(spec.getName(), runtime);
        return runtime;
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
        return this.runtimes.computeIfAbsent(name, (n) -> {
            throw new RuntimeException(String.format("Failed to find runtime with name: %s", n));
        });
    }

    @Override
    @Nullable
    public final D findByName(final String name) {
        return this.runtimes.get(name);
    }

    protected abstract B createBuilder();

    protected abstract void bakeDefinition(D definition);

    public final void bakeDefinitions() {
        this.runtimes.values()
                .stream()
                .filter(def -> !(def instanceof IDelegatingRuntimeDefinition))
                .forEach(this::bakeDefinition);
    }

    public final void bakeDelegateDefinitions() {
        this.runtimes.values()
                .stream()
                .filter(def -> def instanceof IDelegatingRuntimeDefinition)
                .forEach(this::bakeDefinition);
    }

    @Override
    @NotNull
    public Set<D> findIn(final Configuration configuration) {
        final Set<D> directDependency = configuration.getAllDependencies().
                stream().flatMap(dep -> getRuntimes().get().values().stream().filter(runtime -> runtime.getReplacedDependency().equals(dep)))
                .collect(Collectors.toSet());

        if (directDependency.isEmpty())
            return directDependency;

        return project.getConfigurations().stream()
                .filter(config -> config.getHierarchy().contains(configuration))
                .flatMap(config -> config.getAllDependencies().stream())
                .flatMap(dep -> getRuntimes().get().values().stream().filter(runtime -> runtime.getReplacedDependency().equals(dep)))
                .collect(Collectors.toSet());
    }

    protected final TaskProvider<DownloadAssets> createDownloadAssetsTasks(final CommonRuntimeSpecification specification, final Map<String, File> data, final File runtimeDirectory, final VersionJson versionJson) {
        return specification.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(specification, "downloadAssets"), DownloadAssets.class, task -> {
            task.getVersionJson().set(versionJson);

            configureCommonRuntimeTaskParameters(task, data, "downloadAssets", specification, runtimeDirectory);
            task.getOutputDirectory().set(task.getStepsDirectory().map(dir -> dir.dir("downloadAssets")));
        });
    }

    protected final TaskProvider<ExtractNatives> createExtractNativesTasks(final CommonRuntimeSpecification specification, final Map<String, File> data, final File runtimeDirectory, final VersionJson versionJson) {
        return specification.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(specification, "extractNatives"), ExtractNatives.class, task -> {
            task.getVersionJson().set(versionJson);

            configureCommonRuntimeTaskParameters(task, data, "extractNatives", specification, runtimeDirectory);
            task.getOutputDirectory().set(task.getStepsDirectory().map(dir -> dir.dir("downloadAssets")));
        });
    }
}
