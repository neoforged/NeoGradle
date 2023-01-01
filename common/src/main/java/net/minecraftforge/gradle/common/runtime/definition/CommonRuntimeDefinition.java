package net.minecraftforge.gradle.common.runtime.definition;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            throw new IllegalArgumentException("No task with name " + name + " found in runtime " + specification.getName());
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

    @Override
    @NotNull
    public final Map<String, String> getMappingVersionData() {
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
}
