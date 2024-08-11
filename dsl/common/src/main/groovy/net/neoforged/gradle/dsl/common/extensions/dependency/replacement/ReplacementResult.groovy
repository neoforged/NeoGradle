package net.neoforged.gradle.dsl.common.extensions.dependency.replacement

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Defines a result of a dependency replacement.
 */
@CompileStatic
class ReplacementResult {

    private final Project project
    @Nullable
    private final TaskProvider<? extends WithOutput> sourcesJar
    private final TaskProvider<? extends WithOutput> rawJar
    private final Configuration sdk
    private final Configuration dependencies
    private final Set<TaskProvider<? extends Task>> additionalIdePostSyncTasks

    ReplacementResult(
            Project project,
            @Nullable TaskProvider<? extends WithOutput> sourcesJar,
            TaskProvider<? extends WithOutput> rawJar,
            Configuration sdk,
            Configuration dependencies,
            Set<TaskProvider<? extends Task>> additionalTasks
    ) {
        this.project = project
        this.sourcesJar = sourcesJar
        this.rawJar = rawJar
        this.sdk = sdk
        this.dependencies = dependencies
        this.additionalIdePostSyncTasks = additionalTasks
    }

    ReplacementResult(
            Project project,
            TaskProvider<? extends WithOutput> rawJar,
            Configuration sdk,
            Configuration dependencies,
            Set<TaskProvider<? extends Task>> additionalTasks
    ) {
        this.project = project
        this.sourcesJar = null
        this.rawJar = rawJar
        this.sdk = sdk
        this.dependencies = dependencies
        this.additionalIdePostSyncTasks = additionalTasks
    }


    /**
     * @returns The project inside of which a dependency replacement is being performed.
     */
    @NotNull
    Project getProject() {
        return project
    }

    /**
     * @returns The task which produces the sources jar for the dependency replacement.
     */
    @Nullable
    TaskProvider<? extends WithOutput> getSourcesJar() {
        return sourcesJar
    }

    /**
     * @returns The task which produces the raw jar for the dependency replacement.
     */
    @NotNull
    TaskProvider<? extends WithOutput> getRawJar() {
        return rawJar
    }

    /**
     * @returns The configuration in which the SDK is added.
     */
    @NotNull
    Configuration getSdk() {
        return sdk
    }

    /**
     * @returns The configuration in which additional dependencies are added.
     */
    @NotNull
    Configuration getDependencies() {
        return dependencies
    }

    /**
     * @returns The additional tasks which should be executed after the IDE sync.
     */
    @NotNull
    Set<TaskProvider<? extends Task>> getAdditionalIdePostSyncTasks() {
        return additionalIdePostSyncTasks
    }
}
