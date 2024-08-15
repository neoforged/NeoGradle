package net.neoforged.gradle.common.runtime.definition;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

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

    @NotNull
    private final ConfigurableFileCollection allDependencies;
    
    @NotNull
    private final Provider<VersionJson> versionJson;

    protected CommonRuntimeDefinition(
            @NotNull final S specification,
            @NotNull final LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs,
            @NotNull final TaskProvider<? extends ArtifactProvider> sourceJarTask,
            @NotNull final TaskProvider<? extends ArtifactProvider> rawJarTask,
            @NotNull final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks,
            @NotNull final Configuration minecraftDependenciesConfiguration,
            @NotNull final Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer,
            @NotNull final Provider<VersionJson> versionJson) {
        this.specification = specification;
        this.taskOutputs = taskOutputs;
        this.sourceJarTask = sourceJarTask;
        this.rawJarTask = rawJarTask;
        this.gameArtifactProvidingTasks = gameArtifactProvidingTasks;
        this.minecraftDependenciesConfiguration = minecraftDependenciesConfiguration;
        this.associatedTaskConsumer = associatedTaskConsumer;
        this.versionJson = versionJson;

        this.allDependencies = specification.getProject().files();
        this.allDependencies.from(getMinecraftDependenciesConfiguration());
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
    public abstract TaskProvider<DownloadAssets> getAssets();

    @NotNull
    public abstract TaskProvider<ExtractNatives> getNatives();
    
    @NotNull
    public Provider<VersionJson> getVersionJson() {
        return versionJson;
    }

    @NotNull
    @Override
    public final ConfigurableFileCollection getAllDependencies() {
        return allDependencies;
    }

    public void configureRun(RunImpl run) {
        final MapProperty<String, String> runtimeInterpolationData = getSpecification().getProject().getObjects().mapProperty(String.class, String.class);
        buildRunInterpolationData(run, runtimeInterpolationData);

        runtimeInterpolationData.put("source_roots", RunsUtil.buildGradleModClasses(run.getModSources().all()));

        run.getJvmArguments().addAll(
                TransformerUtils.ifTrue(
                        run.getIsClient(),
                        getVersionJson().map(VersionJson::getPlatformJvmArgs)
                )
        );

        run.overrideJvmArguments(interpolate(run.getJvmArguments(), runtimeInterpolationData));
        run.overrideArguments(interpolate(run.getArguments(), runtimeInterpolationData));
        run.overrideEnvironmentVariables(interpolate(run.getEnvironmentVariables(), runtimeInterpolationData));
        run.overrideSystemProperties(interpolate(run.getSystemProperties(), runtimeInterpolationData));

        run.getDependsOn().addAll(
                TransformerUtils.ifTrue(
                        run.getIsClient().flatMap(TransformerUtils.or(run.getIsDataGenerator())),
                        getAssets(),
                        getNatives()
                )
        );
    }

    protected void buildRunInterpolationData(RunImpl run, @NotNull MapProperty<String, String> interpolationData) {
        interpolationData.put("runtime_name", specification.getVersionedName());
        interpolationData.put("mc_version", specification.getMinecraftVersion());
        interpolationData.put("assets_root", DownloadAssets.getAssetsDirectory(specification.getProject())
                .map(Directory::getAsFile)
                .map(File::getAbsolutePath));

        interpolationData.put("asset_index",
                getAssets().flatMap(DownloadAssets::getAssetIndexTargetFile).map(RegularFile::getAsFile).map(File::getName).map(s -> s.substring(0, s.lastIndexOf('.'))));
        interpolationData.put("natives", getNatives().flatMap(ExtractNatives::getOutputDirectory).map(Directory::getAsFile).map(File::getAbsolutePath));
    }

    protected ListProperty<String> interpolate(final ListProperty<String> input, final MapProperty<String, String> values) {
        return interpolate(input, values, "");
    }

    protected ListProperty<String> interpolate(final ListProperty<String> input, final MapProperty<String, String> values, String patternPrefix) {
        final ListProperty<String> delegated = getSpecification().getProject().getObjects().listProperty(String.class);
        delegated.set(input.flatMap(list -> {
            final ListProperty<String> interpolated = getSpecification().getProject().getObjects().listProperty(String.class);
            for (String s : list) {
                interpolated.add(interpolate(s, values, patternPrefix));
            }

            return interpolated;
        }));
        return delegated;
    }

    protected MapProperty<String, String> interpolate(final MapProperty<String, String> input, final MapProperty<String, String> values) {
        return interpolate(input, values, "");
    }

    protected MapProperty<String, String> interpolate(final MapProperty<String, String> input, final MapProperty<String, String> values, String patternPrefix) {
        final MapProperty<String, String> delegated = getSpecification().getProject().getObjects().mapProperty(String.class, String.class);
        delegated.set(input.flatMap(map -> {
            final MapProperty<String, String> interpolated = getSpecification().getProject().getObjects().mapProperty(String.class, String.class);

            for (final Map.Entry<String, String> entry : map.entrySet()) {
                interpolated.put(entry.getKey(), interpolate(entry.getValue(), values, patternPrefix));
            }

            return interpolated;
        }));
        return delegated;
    }

    private static Provider<String> interpolate(final String input, final MapProperty<String, String> values, String patternPrefix) {
        if (input == null)
            throw new IllegalArgumentException("Input cannot be null");

        return values.map(data -> {
            String result = input;
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                result = result.replace(patternPrefix + "{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
        });
    }
}
