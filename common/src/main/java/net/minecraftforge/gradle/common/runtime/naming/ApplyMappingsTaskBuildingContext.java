package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.GameArtifact;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Defines the contextual data that is available to {@link NamingChannelProvider naming channel providers} when they
 * are requested to build a new task that remaps the source jar that is provided via {@link #taskOutputToModify()}
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ApplyMappingsTaskBuildingContext {
    private final @NotNull CommonRuntimeSpec spec;
    private final @NotNull File minecraftCache;
    private final @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks;
    private final @NotNull NamingChannelProvider namingChannelProvider;
    private final @NotNull Map<String, String> mappingVersionData;
    private final @NotNull TaskProvider<? extends IRuntimeTask> taskOutputToModify;
    private final @NotNull Map<GameArtifact, File> gameArtifacts;
    private final @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks;
    private final @NotNull Set<TaskProvider<? extends IRuntimeTask>> additionalRuntimeTasks;
    private final @NotNull Optional<File> intermediaryMappingFile;

    /**
     *
     */
    public ApplyMappingsTaskBuildingContext(
            @NotNull CommonRuntimeSpec spec,
            @NotNull File minecraftCache,
            @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks,
            @NotNull NamingChannelProvider namingChannelProvider,
            @NotNull Map<String, String> mappingVersionData,
            @NotNull TaskProvider<? extends IRuntimeTask> taskOutputToModify,
            @NotNull Map<GameArtifact, File> gameArtifacts,
            @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks,
            @NotNull Set<TaskProvider<? extends IRuntimeTask>> additionalRuntimeTasks,
            @NotNull Optional<File> intermediaryMappingFile) {
        this.spec = spec;
        this.minecraftCache = minecraftCache;
        this.pipelineTasks = pipelineTasks;
        this.namingChannelProvider = namingChannelProvider;
        this.mappingVersionData = mappingVersionData;
        this.taskOutputToModify = taskOutputToModify;
        this.gameArtifacts = gameArtifacts;
        this.gameArtifactTasks = gameArtifactTasks;
        this.additionalRuntimeTasks = additionalRuntimeTasks;
        this.intermediaryMappingFile = intermediaryMappingFile;
    }

    public ApplyMappingsTaskBuildingContext(@NotNull CommonRuntimeSpec spec,
                                            @NotNull File minecraftCache,
                                            @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks,
                                            @NotNull NamingChannelProvider namingChannelProvider,
                                            @NotNull Map<String, String> mappingVersionData,
                                            @NotNull TaskProvider<? extends IRuntimeTask> taskOutputToModify,
                                            @NotNull Map<GameArtifact, File> gameArtifacts,
                                            @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks,
                                            @NotNull Optional<File> intermediaryMappingFile) {
        this(spec, minecraftCache, pipelineTasks, namingChannelProvider, mappingVersionData, taskOutputToModify, gameArtifacts, gameArtifactTasks, new HashSet<>(), intermediaryMappingFile);
    }

    public void withAdditionalRuntimeTask(final TaskProvider<? extends IRuntimeTask> task) {
        additionalRuntimeTasks.add(task);
    }

    public TaskProvider<? extends IRuntimeTask> gameArtifactTask(final GameArtifact artifact) {
        return gameArtifactTasks.computeIfAbsent(artifact, a -> {
            throw new IllegalStateException(String.format("No task found for game artifact %s", a));
        });
    }

    public @NotNull CommonRuntimeSpec spec() {
        return spec;
    }

    public @NotNull File minecraftCache() {
        return minecraftCache;
    }

    public @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks() {
        return pipelineTasks;
    }

    public @NotNull NamingChannelProvider namingChannelProvider() {
        return namingChannelProvider;
    }

    public @NotNull Map<String, String> mappingVersionData() {
        return mappingVersionData;
    }

    public @NotNull Provider<? extends IRuntimeTask> taskOutputToModify() {
        return taskOutputToModify;
    }

    public @NotNull Map<GameArtifact, File> gameArtifacts() {
        return gameArtifacts;
    }

    public @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks() {
        return gameArtifactTasks;
    }

    public @NotNull Set<TaskProvider<? extends IRuntimeTask>> additionalRuntimeTasks() {
        return additionalRuntimeTasks;
    }

    public @NotNull Optional<File> intermediaryMappingFile() {
        return intermediaryMappingFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplyMappingsTaskBuildingContext)) return false;

        ApplyMappingsTaskBuildingContext that = (ApplyMappingsTaskBuildingContext) o;

        if (!spec.equals(that.spec)) return false;
        if (!minecraftCache.equals(that.minecraftCache)) return false;
        if (!pipelineTasks.equals(that.pipelineTasks)) return false;
        if (!namingChannelProvider.equals(that.namingChannelProvider)) return false;
        if (!mappingVersionData.equals(that.mappingVersionData)) return false;
        if (!taskOutputToModify.equals(that.taskOutputToModify)) return false;
        if (!gameArtifacts.equals(that.gameArtifacts)) return false;
        if (!gameArtifactTasks.equals(that.gameArtifactTasks)) return false;
        if (!additionalRuntimeTasks.equals(that.additionalRuntimeTasks)) return false;
        return intermediaryMappingFile.equals(that.intermediaryMappingFile);
    }

    @Override
    public int hashCode() {
        int result = spec.hashCode();
        result = 31 * result + minecraftCache.hashCode();
        result = 31 * result + pipelineTasks.hashCode();
        result = 31 * result + namingChannelProvider.hashCode();
        result = 31 * result + mappingVersionData.hashCode();
        result = 31 * result + taskOutputToModify.hashCode();
        result = 31 * result + gameArtifacts.hashCode();
        result = 31 * result + gameArtifactTasks.hashCode();
        result = 31 * result + additionalRuntimeTasks.hashCode();
        result = 31 * result + intermediaryMappingFile.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RenamingTaskBuildingContext{" +
                "spec=" + spec +
                ", minecraftCache=" + minecraftCache +
                ", pipelineTasks=" + pipelineTasks +
                ", namingChannelProvider=" + namingChannelProvider +
                ", mappingVersionData=" + mappingVersionData +
                ", taskOutputToModify=" + taskOutputToModify +
                ", gameArtifacts=" + gameArtifacts +
                ", gameArtifactTasks=" + gameArtifactTasks +
                ", additionalRuntimeTasks=" + additionalRuntimeTasks +
                ", intermediaryMappingFile=" + intermediaryMappingFile +
                '}';
    }
}
