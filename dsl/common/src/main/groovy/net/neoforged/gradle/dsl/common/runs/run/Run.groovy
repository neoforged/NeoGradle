package net.neoforged.gradle.dsl.common.runs.run


import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.NamedDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.runs.type.RunType
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.internal.impldep.com.fasterxml.jackson.databind.annotation.JsonAppend.Prop
import org.jetbrains.annotations.NotNull

/**
 * Defines an object which represents a single configuration for running the game.
 * Gradle tasks, IDE run configurations, and other run configurations are all created from this object.
 */
@CompileStatic
interface Run extends BaseDSLElement<Run>, NamedDSLElement {

    /**
     * Defines the environment variables that are passed to the JVM when running the game.
     *
     * @return The environment variables that are passed to the JVM when running the game.
     */
    @Input
    @DSLProperty
    abstract MapProperty<String, String> getEnvironmentVariables();

    /**
     * Defines the main class that is executed when the game is started.
     *
     * @return The main class that is executed when the game is started.
     */
    @Input
    @DSLProperty
    abstract Property<String> getMainClass();

    /**
     * Indicates if all the projects in the current Gradle project should be build ahead of running the game.
     *
     * @return {@code true} if all the projects in the current Gradle project should be build ahead of running the game; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    abstract Property<Boolean> getShouldBuildAllProjects();

    /**
     * Defines the program arguments that are passed to the application when running the game.
     *
     * @return The program arguments that are passed to the application when running the game.
     */
    @Input
    @DSLProperty
    abstract ListProperty<String> getProgramArguments();

    /**
     * Defines the JVM arguments that are passed to the JVM when running the game.
     *
     * @return The JVM arguments that are passed to the JVM when running the game.
     */
    @Input
    @DSLProperty
    abstract ListProperty<String> getJvmArguments();

    /**
     * Indicates if this run is a single instance run.
     * If this is set to true, then no other copy of this run configuration can be started while this run configuration is running.
     *
     * @return {@code true} if this run is a single instance run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    abstract Property<Boolean> getIsSingleInstance();

    /**
     * Defines the system properties that are passed to the JVM when running the game.
     *
     * @return The system properties that are passed to the JVM when running the game.
     */
    @Input
    @DSLProperty
    abstract MapProperty<String, String> getSystemProperties();

    /**
     * Defines the working directory that is used when running the game.
     *
     * @return The working directory that is used when running the game.
     */
    @OutputDirectory
    @DSLProperty
    abstract DirectoryProperty getWorkingDirectory();

    /**
     * Indicates if this run is a client run.
     *
     * @return {@code true} if this run is a client run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getIsClient();

    /**
     * Indicates if this run is a server run.
     *
     * @return {@code true} if this run is a server run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getIsServer();

    /**
     * Indicates if this run is a unit test run.
     *
     * @return {@code true} if this run is a unit test run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty(propertyName = 'junit')
    @Optional
    abstract Property<Boolean> getIsJUnit();


    /**
     * Indicates if this run is a data generation run.
     *
     * @return {@code true} if this run is a data generation run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getIsDataGenerator();

    /**
     * Indicates if this run is a game test run.
     *
     * @return {@code true} if this run is a game test run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getIsGameTest();

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
     * Defines the source sets that are used as a mod.
     * <p>
     * For changing the mod identifier a source set belongs to see
     * {@link net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet#getModIdentifier RunnableSourceSet#getModIdentifier}.
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
     * {@link net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet#getModIdentifier RunnableSourceSet#getModIdentifier}.
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
     * Defines the run types that are applied to this run.
     *
     * @return The run types that are applied to this run.
     */
    @Nested
    @DSLProperty
    @Optional
    abstract ListProperty<RunType> getRunTypes();

    /**
     * Adds a run type to this run using the run type name.
     *
     * @param runType The run type to add.
     */
    void runType(@NotNull final String string);

    /**
     * Defines the custom dependency handler for each run.
     *
     * @return The dependency handler for the run.
     */
    @Nested
    @DSLProperty
    abstract Property<DependencyHandler> getDependencies();

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
     * Configures the run using the settings of the associated run type.
     * <p/>
     * Picks a run type using the name of this run, if no specific run type has been set.
     */
    abstract void configure();

    /**
     * Configures the run using the type with the specified name.
     * Throwing an exception if no type could be found.
     *
     * @param type The name of the type to use to configure the run.
     */
    abstract void configure(@NotNull final String type);

    /**
     * Configures the run using the given type.
     *
     * @param type The type to use to configure the run.
     */
    abstract void configure(@NotNull final RunType type);

    /**
     * Configures the run using the given type provider.
     * This will realise the provider to query the values of the type.
     *
     * @param typeProvider The type provider to realise and configure with.
     */
    void configure(@NotNull final Provider<RunType> typeProvider);
}
