//file:noinspection unused
package net.minecraftforge.gradle.dsl.common.runtime.naming

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.ProjectGetter
import net.minecraftforge.gradle.dsl.base.util.GameArtifact
import net.minecraftforge.gradle.dsl.common.extensions.Mappings
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import java.util.function.Function

/**
 * Defines the contextual data that is available to {@link NamingChannel naming channel providers} when they
 * are requested to build a new task that (de)obfuscates a jar that is provided via getInputTask()
 */
@CompileStatic
class TaskBuildingContext {
    private final @NotNull Project project;
    private final @NotNull String environmentName;
    private final @NotNull Function<String, String> taskNameBuilder;
    private final @NotNull TaskProvider<? extends WithOutput> taskOutputToModify;
    private final @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks;
    private final @NotNull Map<String, String> versionData;
    private final @NotNull Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks;
    private final @Nullable Definition<? extends Specification> runtimeDefinition;

    TaskBuildingContext(
            @NotNull Project project,
            @NotNull String environmentName,
            @NotNull Function<String, String> taskNameBuilder,
            @NotNull TaskProvider<? extends WithOutput> taskOutputToModify,
            @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks,
            @NotNull Map<String, String> versionData,
            @NotNull Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks) {
        this.project = project;
        this.environmentName = environmentName;
        this.taskNameBuilder = taskNameBuilder;
        this.taskOutputToModify = taskOutputToModify;
        this.gameArtifactTasks = gameArtifactTasks;
        this.versionData = versionData;
        this.additionalRuntimeTasks = additionalRuntimeTasks;
        this.runtimeDefinition = null;
    }

    TaskBuildingContext(
            @NotNull Project project,
            @NotNull String environmentName,
            @NotNull Function<String, String> taskNameBuilder,
            @NotNull TaskProvider<? extends WithOutput> taskOutputToModify,
            @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks,
            @NotNull Map<String, String> versionData,
            @NotNull Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks,
            @Nullable Definition<? extends Specification> runtimeDefinition) {
        this.project = project
        this.environmentName = environmentName
        this.taskNameBuilder = taskNameBuilder
        this.taskOutputToModify = taskOutputToModify
        this.gameArtifactTasks = gameArtifactTasks
        this.versionData = versionData
        this.additionalRuntimeTasks = additionalRuntimeTasks
        this.runtimeDefinition = runtimeDefinition
    }

    /**
     * Registers an additional task to run, and schedules it for configuration with the current environments properties.
     *
     * @param task The task to add.
     */
    void addTask(final TaskProvider<? extends Runtime> task) {
        additionalRuntimeTasks.add(task);
    }

    /**
     * The current project that is being configured.
     *
     * @return The current project.
     */
    @NotNull
    @ProjectGetter
    Project getProject() {
        return project;
    }

    /**
     * The name of the environment that is being configured.
     *
     * @return The name of the environment.
     */
    @NotNull
    String getEnvironmentName() {
        return environmentName;
    }

    /**
     * A function that can be used to build a task name for a given task type.
     *
     * @return A function that can be used to build a task name for a given task type.
     */
    @NotNull
    Function<String, String> getTaskNameBuilder() {
        return taskNameBuilder
    }

    /**
     * Gives access to a task provider that has as output the games artifact for the current minecraft version and environment.
     *
     * @param artifact The type of the artifact to get.
     * @return A task provider which has as output the games artifact for the current minecraft version and environment.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getGameArtifactTask(final GameArtifact artifact) {
        return gameArtifactTasks.computeIfAbsent(artifact, a -> {
            throw new IllegalStateException(String.format('No task found for game artifact %s. Available are: %s', a, gameArtifactTasks.keySet()));
        });
    }

    /**
     * Gives access to a task provider that has as output the launcher manifest.
     *
     * @return A task provider which has as output the launcher manifest.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getLauncherManifest() {
        return getGameArtifactTask(GameArtifact.LAUNCHER_MANIFEST);
    }

    /**
     * Gives access to a task provider that has as output the current minecraft versions manifest.
     *
     * @return A task provider which has as output the current minecraft versions manifest.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getVersionManifest() {
        return getGameArtifactTask(GameArtifact.VERSION_MANIFEST);
    }

    /**
     * Gives access to a task provider that has as output the current minecraft versions client jar.
     *
     * @return A task provider which has as output the current minecraft versions client jar.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getClientJar() {
        return getGameArtifactTask(GameArtifact.CLIENT_JAR);
    }

    /**
     * Gives access to a task provider that has as output the current minecraft versions server jar.
     *
     * @return A task provider which has as output the current minecraft versions server jar.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getServerJar() {
        return getGameArtifactTask(GameArtifact.SERVER_JAR);
    }

    /**
     * Gives access to a task provider that has as output the current minecraft versions client mappings file.
     *
     * @return A task provider which has as output the current minecraft versions client mappings file.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getClientMappings() {
        return getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS);
    }

    /**
     * Gives access to a task provider that has as output the current minecraft versions server mappings file.
     *
     * @return A task provider which has as output the current minecraft versions server mappings file.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getServerMappings() {
        return getGameArtifactTask(GameArtifact.SERVER_MAPPINGS);
    }

    /**
     * Gives access to the naming channel which was requested to provide a mappings task.
     *
     * @return The naming channel which was requested to provide a mappings task.
     */
    @NotNull NamingChannel getNamingChannel() {
        return getProject().getExtensions().getByType(Mappings.class).getChannel().get();
    }

    /**
     * Gives access to the user configured mappings version information,
     * normally appended with the current minecraft version.
     *
     * @return The user configured mappings version information.
     */
    @NotNull Map<String, String> getMappingVersion() {
        return versionData;
    }

    /**
     * Gives access to a task provider that has as output the jar to which the mappings should be applied.
     *
     * @return A task provider which has as output the jar to which the mappings should be applied.
     */
    @NotNull TaskProvider<? extends WithOutput> getInputTask() {
        return taskOutputToModify;
    }

    /**
     * Gives access to an optional which holds the runtime definition that is being configured.
     * This is only present when the runtime definition is being configured.
     *
     * @return An optional which holds the runtime definition that is being configured.
     */
    @NotNull Optional<? extends Definition<? extends Specification>> getRuntimeDefinition() {
        return Optional.ofNullable(runtimeDefinition)
    }
}
