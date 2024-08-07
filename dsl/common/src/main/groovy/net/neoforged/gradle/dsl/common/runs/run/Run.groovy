package net.neoforged.gradle.dsl.common.runs.run


import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.NamedDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.runs.RunSpecification
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.annotations.NotNull

/**
 * Defines an object which represents a single configuration for running the game.
 * Gradle tasks, IDE run configurations, and other run configurations are all created from this object.
 */
@CompileStatic
interface Run extends BaseDSLElement<Run>, NamedDSLElement, RunSpecification {

    /**
     * Gives access to the application arguments for the run type.
     *
     * @return The property which holds the application arguments.
     * @deprecated Use {@link #getArguments()} instead.
     */
    @Deprecated
    @DSLProperty
    @Internal
    abstract ListProperty<String> getProgramArguments();

    /**
     * Defines the working directory that is used when running the game.
     *
     * @return The working directory that is used when running the game.
     */
    @Internal
    @DSLProperty
    abstract DirectoryProperty getWorkingDirectory();

    /**
     * @returns the RenderDoc options for this run. RenderDoc can only be used on client runs.
     */
    @Nested
    @DSLProperty
    abstract RunRenderDocOptions getRenderDoc();

    /**
     * @returns the DevLogin options for this run.
     */
    @Nested
    @DSLProperty
    abstract RunDevLoginOptions getDevLogin();

    /**
     * @returns whether or not this run should be exported to the IDE.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getShouldExportToIDE();

    /**
     * Defines the source sets that are used as a mod.
     * <p>
     * For changing the mod identifier a source set belongs to see
     * {@link net.neoforged.gradle.dsl.common.extensions.sourceset.RunnableSourceSet#getModIdentifier RunnableSourceSet#getModIdentifier}.
     *
     * @return The source sets that are used as a mod.
     */
    @Nested
    @DSLProperty
    abstract RunSourceSets getModSources();

    /**
     * Adds a source set to the mod sources.
     *
     * @param sourceSet The source set to add.
     */
    void modSource(@NotNull final SourceSet sourceSet)

    /**
     * Adds source sets to the mod sources.
     *
     * @param sourceSets The source sets to add.
     */
    void modSources(@NotNull final SourceSet... sourceSets)

    /**
     * Adds source sets to the mod sources.
     *
     * @param sourceSets The source sets to add.
     */
    void modSources(@NotNull final Iterable<? extends SourceSet> sourceSets)

    /**
     * Defines the source sets that are used as a test.
     * <p>
     * For changing the mod identifier a source set belongs to see
     * {@link net.neoforged.gradle.dsl.common.extensions.sourceset.RunnableSourceSet#getModIdentifier RunnableSourceSet#getModIdentifier}.
     *
     * @return The source sets that are used as a mod.
     */
    @Nested
    @DSLProperty
    abstract RunSourceSets getUnitTestSources();

    /**
     * Adds a source set to the unit test sources.
     *
     * @param sourceSet The source set to add.
     */
    void unitTestSource(@NotNull final SourceSet sourceSet)

    /**
     * Adds source sets to the unit test sources.
     *
     * @param sourceSets The source sets to add.
     */
    void unitTestSources(@NotNull final SourceSet... sourceSets)

    /**
     * Adds source sets to the unit test sources.
     *
     * @param sourceSets The source sets to add.
     */
    void unitTestSources(@NotNull final Iterable<? extends SourceSet> sourceSets)

    /**
     * @return a scope configuration object that an be used to limit tests further then the test sourcesets alone.
     */
    @DSLProperty
    @Nested
    abstract RunTestScope getTestScope()

    /**
     * Gives access to the classpath for this run.
     *
     * @return The property which holds the classpath.
     */
    @InputFiles
    @Classpath
    @DSLProperty
    abstract ConfigurableFileCollection getRuntimeClasspath();

    /**
     * Gives access to the runtime classpath elements for this run.
     *
     * @return A provider that provides the classpath elements.
     * @implNote This is a loosely coupled provider, because if you call {@link ConfigurableFileCollection#getElements()} directly, it will return a provider that is not transformable.
     */
    @Internal
    abstract Provider<Set<FileSystemLocation>> getRuntimeClasspathElements()

    /**
     * Gives access to the classpath for this run.
     *
     * @return The property which holds the classpath.
     */
    @InputFiles
    @Classpath
    @DSLProperty
    abstract ConfigurableFileCollection getTestRuntimeClasspath();

    /**
     * Gives access to the test runtime classpath elements for this run.
     *
     * @return A provider that provides the classpath elements.
     * @implNote This is a loosely coupled provider, because if you call {@link ConfigurableFileCollection#getElements()} directly, it will return a provider that is not transformable.
     */
    @Internal
    abstract Provider<Set<FileSystemLocation>> getTestRuntimeClasspathElements()

    /**
     * Gives access to the compile classpath classpath for this run.
     *
     * @return The property which holds the compile classpath.
     */
    @InputFiles
    @Classpath
    @DSLProperty
    abstract ConfigurableFileCollection getCompileClasspath();

    /**
     * Gives access to the compile classpath elements for this run.
     *
     * @return A provider that provides the classpath elements.
     * @implNote This is a loosely coupled provider, because if you call {@link ConfigurableFileCollection#getElements()} directly, it will return a provider that is not transformable.
     */
    @Internal
    abstract Provider<Set<FileSystemLocation>> getCompileClasspathElements()

    /**
     * Gives access to the compile classpath classpath for this run.
     *
     * @return The property which holds the compile classpath.
     */
    @InputFiles
    @Classpath
    @DSLProperty
    abstract ConfigurableFileCollection getTestCompileClasspath()

    /**
     * Gives access to the test compile classpath elements for this run.
     *
     * @return A provider that provides the classpath elements.
     * @implNote This is a loosely coupled provider, because if you call {@link ConfigurableFileCollection#getElements()} directly, it will return a provider that is not transformable.
     */
    @Internal
    abstract Provider<Set<FileSystemLocation>> getTestCompileClasspathElements()

    /**
     * Gives access to the sdk classpath for this run.
     *
     * @return The property which holds the mdk classpath.
     */
    @InputFiles
    @Classpath
    @DSLProperty
    abstract ConfigurableFileCollection getSdkClasspath()

    /**
     * Gives access to the sdk classpath elements for this run.
     *
     * @return A provider that provides the classpath elements.
     * @implNote This is a loosely coupled provider, because if you call {@link ConfigurableFileCollection#getElements()} directly, it will return a provider that is not transformable.
     */
    @Internal
    abstract Provider<Set<FileSystemLocation>> getSdkClasspathElements()

    /**
     * Adds a run type to this run using the run type name.
     *
     * @param runType The run type to add.
     */
    void runType(@NotNull final String string);

    /**
     * Adds a run type to this run.
     *
     * @param runType The run type to add.
     */
    void run(@NotNull final String name);

    /**
     * Defines the custom dependency handler for each run.
     *
     * @return The dependency handler for the run.
     */
    @Nested
    @DSLProperty
    abstract DependencyHandler getDependencies();

    /**
     * Indicates if this run should automatically be configured.
     *
     * @return The property which indicates if this run should automatically be configured.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getConfigureAutomatically();

    /**
     * Indicates if this run should automatically be configured by the type of the same name.
     *
     * @return The property which indicates if this run should automatically be configured by the type of the same name.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getConfigureFromTypeWithName();

    /**
     * Indicates if this run should automatically be configured by its dependent runtimes.
     *
     * @return The property which indicates if this run should automatically be configured by its dependent runtimes.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getConfigureFromDependencies();

    /**
     * @return The tasks that this run depends on.
     */
    @Internal
    @DSLProperty
    @Optional
    abstract SetProperty<Task> getDependsOn();

    /**
     * @return The tasks that should be ran on post sync.
     */
    @Internal
    @DSLProperty
    @Optional
    abstract SetProperty<Task> getPostSyncTasks();

    /**
     * Configures the run using the settings of the associated run type.
     * <p/>
     * If no configuration specification is set then this will try to lookup a run type with the same name as the run.
     */
    abstract void configure();

    /**
     * Configures the run using the type with the specified name.
     * Throwing an exception if no type could be found.
     *
     * @param type The name of the type to use to configure the run.
     */
    @Deprecated
    abstract void configure(@NotNull final String type);

    /**
     * Configures the run using the given type.
     *
     * @param type The type to use to configure the run.
     */
    abstract void configure(@NotNull final RunSpecification type);

    /**
     * Configures the run using the given type provider.
     *
     * @param typeProvider The type provider to realise and configure with.
     */
    void configure(@NotNull final Provider<? extends RunSpecification> typeProvider);
}
