package net.neoforged.gradle.common.runtime.definition;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.tasks.ArtifactFromOutput;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CommonRuntimeDefinition<S extends CommonRuntimeSpecification> implements Definition<S> {

    @NotNull
    private final S specification;

    @NotNull
    private final LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs;

    @NotNull
    private final TaskProvider<? extends ArtifactProvider> sourceJarTask;

    @NotNull
    private final TaskProvider<? extends ArtifactProvider> rawJarTask;

    @NotNull
    private final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks;

    @NotNull
    private final Configuration minecraftDependenciesConfiguration;

    @NotNull
    private final Map<String, String> mappingVersionData = Maps.newHashMap();

    @NotNull
    private final Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer;

    @Nullable
    private Dependency replacedDependency = null;

    protected CommonRuntimeDefinition(
            @NotNull final S specification,
            @NotNull final LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs,
            @NotNull final TaskProvider<? extends ArtifactProvider> sourceJarTask,
            @NotNull final TaskProvider<? extends ArtifactProvider> rawJarTask,
            @NotNull final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks,
            @NotNull final Configuration minecraftDependenciesConfiguration,
            @NotNull final Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer) {
        this.specification = specification;
        this.taskOutputs = taskOutputs;
        this.sourceJarTask = sourceJarTask;
        this.rawJarTask = rawJarTask;
        this.gameArtifactProvidingTasks = gameArtifactProvidingTasks;
        this.minecraftDependenciesConfiguration = minecraftDependenciesConfiguration;
        this.associatedTaskConsumer = associatedTaskConsumer;
    }

    @Override
    @NotNull
    public final TaskProvider<? extends WithOutput> getTask(String name) {
        final String taskName = CommonRuntimeUtils.buildTaskName(this, name);
        if (!taskOutputs.containsKey(taskName)) {
            throw new IllegalArgumentException("No task with name " + name + " found in runtime " + specification.getVersionedName());
        }

        return taskOutputs.get(taskName);
    }

    @Override
    @NotNull
    public final TaskProvider<? extends ArtifactProvider> getRawJarTask() {
        return rawJarTask;
    }

    @Override
    @NotNull
    public final S getSpecification() {
        return specification;
    }

    @Override
    @NotNull
    public final LinkedHashMap<String, TaskProvider<? extends WithOutput>> getTasks() {
        return taskOutputs;
    }

    @Override
    @NotNull
    public final TaskProvider<? extends ArtifactProvider> getSourceJarTask() {
        return sourceJarTask;
    }

    @Override
    @NotNull
    public final Map<GameArtifact, TaskProvider<? extends WithOutput>> getGameArtifactProvidingTasks() {
        return gameArtifactProvidingTasks;
    }

    @Override
    @NotNull
    public final Configuration getMinecraftDependenciesConfiguration() {
        return minecraftDependenciesConfiguration;
    }

    @Override
    @NotNull
    public final Dependency getReplacedDependency() {
        if (this.replacedDependency == null)
            throw new IllegalStateException("No dependency has been replaced yet.");

        return this.replacedDependency;
    }

    public void setReplacedDependency(@NotNull final Dependency dependency) {
        this.replacedDependency = dependency;
    }

    public void onRepoWritten(@NotNull final TaskProvider<? extends WithOutput> finalRepoWritingTask) {
    }

    @Override
    @NotNull
    public Map<String, String> getMappingVersionData() {
        return mappingVersionData;
    }

    public final void setMappingVersionData(@NotNull final Map<String, String> data) {
        mappingVersionData.clear();
        mappingVersionData.putAll(data);
    }

    @Override
    public void configureAssociatedTask(@NotNull TaskProvider<? extends Runtime> runtimeTask) {
        this.associatedTaskConsumer.accept(runtimeTask);
    }

    @NotNull
    public abstract TaskProvider<DownloadAssets> getAssetsTaskProvider();

    @NotNull
    public abstract TaskProvider<ExtractNatives> getNativesTaskProvider();

    public void configureRun(RunImpl run) {
        run.getExtensions().getExtraProperties().set("runtimeDefinition", this);

        final Map<String, String> runtimeInterpolationData = buildRunInterpolationData();

        final Map<String, String> workingInterpolationData = new HashMap<>(runtimeInterpolationData);
        workingInterpolationData.put("source_roots",  Stream.concat(
                run.getModSources().get().stream().map(source -> source.getOutput().getResourcesDir()),
                run.getModSources().get().stream().map(source -> source.getOutput().getClassesDirs().getFiles()).flatMap(Collection::stream)
        ).map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));

        run.overrideJvmArguments(interpolate(run.getJvmArguments(), workingInterpolationData));
        run.overrideProgramArguments(interpolate(run.getProgramArguments(), workingInterpolationData));
        run.overrideEnvironmentVariables(interpolate(run.getEnvironmentVariables(), workingInterpolationData));
        run.overrideSystemProperties(interpolate(run.getSystemProperties(), workingInterpolationData));

        run.dependsOn(getAssetsTaskProvider(), getNativesTaskProvider());
    }

    protected Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = Maps.newHashMap();

        interpolationData.put("runtime_name", specification.getVersionedName());
        interpolationData.put("mc_version", specification.getMinecraftVersion());
        interpolationData.put("assets_root", getAssetsTaskProvider().get().getOutputDirectory().get().getAsFile().getAbsolutePath());
        interpolationData.put("asset_index", getAssetsTaskProvider().get().getAssetIndexFile().get().getAsFile().getName().substring(0, getAssetsTaskProvider().get().getAssetIndexFile().get().getAsFile().getName().lastIndexOf('.')));
        interpolationData.put("natives", getNativesTaskProvider().get().getOutputDirectory().get().getAsFile().getAbsolutePath());

        return interpolationData;
    }

    public void onBake(final NamingChannel namingChannel, final File runtimeDirectory) {

    }

    protected ListProperty<String> interpolate(final ListProperty<String> input, final Map<String, String> values) {
        return interpolate(input, values, "");
    }

    protected ListProperty<String> interpolate(final ListProperty<String> input, final Map<String, String> values, String patternPrefix) {
        final ListProperty<String> delegated = getSpecification().getProject().getObjects().listProperty(String.class);
        delegated.set(input.map(list -> list.stream().map(s -> interpolate(s, values, patternPrefix)).collect(Collectors.toList())));
        return delegated;
    }

    protected MapProperty<String, String> interpolate(final MapProperty<String, String> input, final Map<String, String> values) {
        return interpolate(input, values, "");
    }

    protected MapProperty<String, String> interpolate(final MapProperty<String, String> input, final Map<String, String> values, String patternPrefix) {
        final MapProperty<String, String> delegated = getSpecification().getProject().getObjects().mapProperty(String.class, String.class);
        delegated.set(input.map(list -> list.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> interpolate(e.getValue(), values, patternPrefix)))));
        return delegated;
    }

    private static String interpolate(final String input, final Map<String, String> values, String patternPrefix) {
        if (input == null)
            throw new IllegalArgumentException("Input cannot be null");

        String result = input;
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(patternPrefix + "{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
