package net.neoforged.gradle.dsl.common.runtime.definition

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.spec.Specification
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.util.GameArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.annotations.NotNull

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
     * Returns the task which creates a file listing all the libraries used by the runtime.
     *
     * @return The task which creates a file listing all the libraries used by the runtime.
     */
    @NotNull
    abstract TaskProvider<? extends WithOutput> getListLibrariesTaskProvider();

    /**
     * Returns all the files which should be considered dependencies of the runtime.
     * This includes the runtime's own dependencies, as well as the dependencies of the minecraft dependency.
     *
     * @return The dependencies of the runtime.
     */
    @NotNull
    ConfigurableFileCollection getAllDependencies()

    /**
     * A collection of files which need to be added to the recompile classpath,
     * for the recompile phase to succeed.
     *
     * @return The file collection with the additional jars which need to be added
     */
    @NotNull
    FileCollection getAdditionalRecompileDependencies();

    /**
     * Adds a dependency to the recompile classpath.
     *
     * @param dependency The dependency to add.
     */
    void additionalRecompileDependency(Provider<RegularFile> dependency);

    /**
     * Adds dependencies to the recompile classpath.
     *
     * @param dependencies The dependencies to add.
     */
    void additionalRecompileDependencies(FileCollection dependencies);

    /**
     * A collection of source files which need to be added to the compile sources.
     * These sources are removed again from the compiled jar.
     *
     * @return The file collection with the additional sources which need to be added
     */
    @NotNull
    FileCollection getAdditionalCompileSources();

    /**
     * Adds a source to the compile sources.
     *
     * @param source The source to add.
     */
    void additionalCompileSource(Provider<RegularFile> source);

    /**
     * Adds sources to the compile sources.
     *
     * @param sources The sources to add.
     */
    void additionalCompileSources(FileCollection sources);
}
