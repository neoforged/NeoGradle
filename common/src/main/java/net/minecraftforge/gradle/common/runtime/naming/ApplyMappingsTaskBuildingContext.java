package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import org.gradle.api.Project;
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
public class ApplyMappingsTaskBuildingContext {
    private final @NotNull Project project;
    private final @NotNull String environmentName;
    private final @NotNull TaskProvider<? extends ITaskWithOutput> taskOutputToModify;
    private final @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks;
    private final @NotNull Map<String, String> versionData;
    private final @NotNull Set<TaskProvider<? extends IRuntimeTask>> additionalRuntimeTasks;

    public ApplyMappingsTaskBuildingContext(
            @NotNull Project project,
            @NotNull String environmentName,
            @NotNull TaskProvider<? extends ITaskWithOutput> taskOutputToModify,
            @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks,
            @NotNull Map<String, String> versionData,
            @NotNull Set<TaskProvider<? extends IRuntimeTask>> additionalRuntimeTasks) {
        this.project = project;
        this.environmentName = environmentName;
        this.taskOutputToModify = taskOutputToModify;
        this.gameArtifactTasks = gameArtifactTasks;
        this.versionData = versionData;
        this.additionalRuntimeTasks = additionalRuntimeTasks;
    }

    public ApplyMappingsTaskBuildingContext(@NotNull Project project,
                                            @NotNull String environmentName,
                                            @NotNull TaskProvider<? extends ITaskWithOutput> taskOutputToModify,
                                            @NotNull Map<String, String> versionData,
                                            @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks) {
        this(project, environmentName, taskOutputToModify, gameArtifactTasks, versionData, new HashSet<>());
    }

    public void withAdditionalRuntimeTask(final TaskProvider<? extends IRuntimeTask> task) {
        additionalRuntimeTasks.add(task);
    }

    public Project project() {
        return project;
    }

    public String environmentName() {
        return environmentName;
    }

    public TaskProvider<? extends ITaskWithOutput> gameArtifactTask(final GameArtifact artifact) {
        return gameArtifactTasks.computeIfAbsent(artifact, a -> {
            throw new IllegalStateException(String.format("No task found for game artifact %s", a));
        });
    }

    public @NotNull NamingChannelProvider namingChannelProvider() {
        return project().getExtensions().getByType(MappingsExtension.class).getMappingChannel().get();
    }

    public @NotNull Map<String, String> mappingVersionData() {
        return versionData;
    }

    public @NotNull TaskProvider<? extends ITaskWithOutput> taskOutputToModify() {
        return taskOutputToModify;
    }

    public @NotNull Set<TaskProvider<? extends IRuntimeTask>> additionalRuntimeTasks() {
        return additionalRuntimeTasks;
    }

}
