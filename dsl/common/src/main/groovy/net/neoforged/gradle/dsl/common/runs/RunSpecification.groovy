package net.neoforged.gradle.dsl.common.runs

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Definition of a run specification.
 */
interface RunSpecification {

    /**
     * Indicates if the run type is only allowed to run once at a time.
     *
     * @return The property which indicates if this is a single instance run type.
     */
    @Input
    @DSLProperty
    abstract Property<Boolean> getIsSingleInstance();

    /**
     * Gives access to the name of the main class on the run type.
     *
     * @return The property which holds the main class name.
     */
    @DSLProperty
    @Input
    abstract Property<String> getMainClass();

    /**
     * Gives access to the application arguments for the run type.
     *
     * @return The property which holds the application arguments.
     */
    @DSLProperty
    @Input
    abstract ListProperty<String> getArguments();

    /**
     * Gives access to the JVM arguments for the run type.
     *
     * @return The property which holds the JVM arguments.
     */
    @DSLProperty
    @Input
    abstract ListProperty<String> getJvmArguments();

    /**
     * Indicates if this run type is for the client.
     *
     * @return The property which indicates if this is a client run type.
     */
    @DSLProperty(propertyName = 'client')
    @Input
    abstract Property<Boolean> getIsClient();

    /**
     * Indicates if this run is a server run.
     *
     * @return {@code true} if this run is a server run; otherwise, {@code false}.
     */
    @DSLProperty(propertyName = 'server')
    @Input
    abstract Property<Boolean> getIsServer();

    /**
     * Indicates if this run is a JUnit run.
     *
     * @return {@code true} if this run is a JUnit run; otherwise, {@code false}.
     */
    @DSLProperty(propertyName = 'junit')
    @Input
    abstract Property<Boolean> getIsJUnit();

    /**
     * Indicates if this run is a data generation run.
     *
     * @return {@code true} if this run is a data generation run; otherwise, {@code false}.
     */
    @DSLProperty(propertyName = 'dataGenerator')
    @Input
    abstract Property<Boolean> getIsDataGenerator();

    /**
     * Indicates if this run is a game test run.
     *
     * @return {@code true} if this run is a game test run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty(propertyName = 'gameTest')
    abstract Property<Boolean> getIsGameTest();

    /**
     * Gives access to the key value pairs which are added as environment variables when an instance of this run type is executed.
     *
     * @return The property which holds the environment variables.
     */
    @DSLProperty
    @Input
    abstract MapProperty<String, String> getEnvironmentVariables();

    /**
     * Gives access to the key value pairs which are added as system properties when an instance of this run type is executed.
     *
     * @return The property which holds the system properties.
     */
    @DSLProperty
    @Input
    abstract MapProperty<String, String> getSystemProperties();

    /**
     * Gives access to the classpath for this run type.
     * Does not contain the full classpath since that is dependent on the actual run environment, but contains the additional classpath elements
     * needed to run the game with this run type.
     *
     * @return The property which holds the classpath.
     */
    @DSLProperty
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    abstract ConfigurableFileCollection getClasspath();
}