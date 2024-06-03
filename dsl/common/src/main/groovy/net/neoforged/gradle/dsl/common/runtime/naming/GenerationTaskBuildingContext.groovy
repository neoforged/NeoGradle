//file:noinspection unused
package net.neoforged.gradle.dsl.common.runtime.naming

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.ProjectGetter
import net.neoforged.gradle.dsl.common.extensions.Mappings
import net.neoforged.gradle.dsl.common.runtime.definition.LegacyDefinition
import net.neoforged.gradle.dsl.common.runtime.spec.LegacySpecification
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.util.GameArtifact
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
class GenerationTaskBuildingContext {
    private final @NotNull Project project;
    private final @NotNull String environmentName;
    private final @NotNull Function<String, String> taskNameBuilder;
    private final @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks;
    private final @Nullable LegacyDefinition<? extends LegacySpecification> runtimeDefinition;

    GenerationTaskBuildingContext(
            @NotNull Project project,
            @NotNull String environmentName,
            @NotNull Function<String, String> taskNameBuilder,
            @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks) {
        this.project = project;
        this.environmentName = environmentName;
        this.taskNameBuilder = taskNameBuilder;
        this.gameArtifactTasks = gameArtifactTasks;
        this.runtimeDefinition = null;
    }

    GenerationTaskBuildingContext(
            @NotNull Project project,
            @NotNull String environmentName,
            @NotNull Function<String, String> taskNameBuilder,
            @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks,
            @Nullable LegacyDefinition<? extends LegacySpecification> runtimeDefinition) {
        this.project = project
        this.environmentName = environmentName
        this.taskNameBuilder = taskNameBuilder
        this.gameArtifactTasks = gameArtifactTasks
        this.runtimeDefinition = runtimeDefinition
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
     * Gives access to an optional which holds the runtime definition that is being configured.
     * This is only present when the runtime definition is being configured.
     *
     * @return An optional which holds the runtime definition that is being configured.
     */
    @NotNull Optional<? extends LegacyDefinition<? extends LegacySpecification>> getRuntimeDefinition() {
        return Optional.ofNullable(runtimeDefinition)
    }
}
