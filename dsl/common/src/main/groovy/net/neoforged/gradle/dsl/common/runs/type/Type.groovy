package net.neoforged.gradle.dsl.common.runs.type

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.NamedDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Defines an object which holds the type of run.
 * This is normally loaded from an userdev artifact.
 * <p>
 * However, for pure vanilla these objects are created in memory specifically for the run.
 */
@CompileStatic
interface Type extends BaseDSLElement<Type>, NamedDSLElement {

    /**
     * Indicates if the run type is only allowed to run once at a time.
     *
     * @return The property which indicates if this is a single instance run type.
     */
    @DSLProperty
    Property<Boolean> getIsSingleInstance();

    /**
     * Gives access to the name of the main class on the run type.
     *
     * @return The property which holds the main class name.
     */
    @DSLProperty
    Property<String> getMainClass();

    /**
     * Gives access to the application arguments for the run type.
     *
     * @return The property which holds the application arguments.
     */
    @DSLProperty
    ListProperty<String> getArguments();

    /**
     * Gives access to the JVM arguments for the run type.
     *
     * @return The property which holds the JVM arguments.
     */
    @DSLProperty
    ListProperty<String> getJvmArguments();

    /**
     * Indicates if this run type is for the client.
     *
     * @return The property which indicates if this is a client run type.
     */
    @DSLProperty
    Property<Boolean> getIsClient();

    /**
     * Indicates if this run is a server run.
     *
     * @return {@code true} if this run is a server run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    Property<Boolean> getIsServer();

    /**
     * Indicates if this run is a data generation run.
     *
     * @return {@code true} if this run is a data generation run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    Property<Boolean> getIsDataGenerator();

    /**
     * Indicates if this run is a game test run.
     *
     * @return {@code true} if this run is a game test run; otherwise, {@code false}.
     */
    @Input
    @DSLProperty
    Property<Boolean> getIsGameTest();

    /**
     * Gives access to the key value pairs which are added as environment variables when an instance of this run type is executed.
     *
     * @return The property which holds the environment variables.
     */
    @DSLProperty
    MapProperty<String, String> getEnvironmentVariables();

    /**
     * Gives access to the key value pairs which are added as system properties when an instance of this run type is executed.
     *
     * @return The property which holds the system properties.
     */
    @DSLProperty
    MapProperty<String, String> getSystemProperties();

    /**
     * Gives access to the classpath for this run type.
     * Does not contain the full classpath since that is dependent on the actual run environment, but contains the additional classpath elements
     * needed to run the game with this run type.
     *
     * @return The property which holds the classpath.
     */
    @DSLProperty
    ConfigurableFileCollection getClasspath();

    /**
     * An optional configurable run adapter which can be used to change the behaviour of already configured runs when the type is applied to them.
     *
     * @return The run adapter.
     */
    @DSLProperty
    @Optional
    Property<RunAdapter> getRunAdapter();

    /**
     * Copies this run type into a new instance.
     *
     * @param other The other run type to copy into.
     */
    void copyTo(Type other);

    /**
     * Copies the data from the given other type into this type.
     *
     * @param other The type to copy from.
     */
    void from(Type other);
}
