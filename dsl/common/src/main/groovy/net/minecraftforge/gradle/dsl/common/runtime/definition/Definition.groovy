package net.minecraftforge.gradle.dsl.common.runtime.definition

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import net.minecraftforge.gradle.dsl.common.util.GameArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

@CompileStatic
interface Definition<S extends Specification> {
    /**
     * Returns the runtimes task with the given name.
     * The given name is prefixed with the name of the runtime, if needed.
     * Invoking this method with the name of a task that is not part of the runtime will result in an {@link IllegalArgumentException exception}.
     *
     * @param name The name of the task to get.
     * @return The named task.
     */
    @NotNull TaskProvider<? extends WithOutput> getTask(String name);

    /**
     * Returns the task which produces the raw jar used for compiling against.
     *
     * @return The raw jar producing taskOutputs.
     */
    @NotNull TaskProvider<? extends ArtifactProvider> getRawJarTask();

    /**
     * Gives access to the specification which created this definition.
     *
     * @return The specification.
     */
    @NotNull
    S getSpecification();

    /**
     * Gives access to the tasks which are part of this runtime.
     *
     * @return The tasks in this runtime.
     */
    @NotNull LinkedHashMap<String, TaskProvider<? extends WithOutput>> getTasks();

    /**
     * Gives access to the task that produces the sources jar.
     *
     * @return The sources jar task.
     */
    @NotNull TaskProvider<? extends ArtifactProvider> getSourceJarTask();

    /**
     * Gives access to the tasks that produce the game artifacts of this runtime.
     *
     * @return The game artifacts.
     */
    @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> getGameArtifactProvidingTasks();

    /**
     * Gives access to a detached configuration which contains the dependencies of the minecraft dependency.
     *
     * @return The minecraft dependency configuration.
     */
    @NotNull Configuration getMinecraftDependenciesConfiguration();

    /**
     * Gives access to the gradle dependency which represents this runtime.
     * If the dependency has not been baked yet, this method will throw an exception.
     *
     * @return The representing gradle dependency.
     */
    @NotNull Dependency getReplacedDependency();

    /**
     * Returns a map of versioning information for the configured naming channel that was used during the baking.
     *
     * @return The baking versioning information for the naming channel.
     */
    @NotNull Map<String, String> getMappingVersionData();

    /**
     * Exposes an entry point which can be used to to configure a task which is associated to the definition.
     *
     * @param runtimeTask The runtime task to configure.
     */
    void configureAssociatedTask(@NotNull final TaskProvider<? extends Runtime> runtimeTask)

    /**
     * Returns the task which produces the runtime mapped jar.
     *
     * @return The runtime mapped jar producing task.
     */
    @NotNull
    abstract TaskProvider<? extends WithOutput> getRuntimeMappedRawJarTaskProvider();

    /**
     * Returns the task which creates a file listing all the libraries used by the runtime.
     *
     * @return The task which creates a file listing all the libraries used by the runtime.
     */
    @NotNull
    abstract TaskProvider<? extends WithOutput> getListLibrariesTaskProvider(); }
