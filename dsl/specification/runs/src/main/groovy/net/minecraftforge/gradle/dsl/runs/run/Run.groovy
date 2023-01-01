package net.minecraftforge.gradle.dsl.runs.run;

import groovy.transform.CompileStatic;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import net.minecraftforge.gradle.dsl.base.util.NamedDSLElement
import net.minecraftforge.gradle.dsl.runs.type.Type
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar;

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
    MapProperty<String, String> getEnvironmentVariables();

    /**
     * Defines the main class that is executed when the game is started.
     *
     * @return The main class that is executed when the game is started.
     */
    @Input
    @DSLProperty
    Property<String> getMainClass();

    /**
     * Indicates if all the projects in the current Gradle project should be build ahead of running the game.
     *
     * @return {@code true} if all the projects in the current Gradle project should be build ahead of running the game; otherwise, {@code false}.
     */
    @DSLProperty
    Property<Boolean> getShouldBuildAllProjects();

    /**
     * Defines the program arguments that are passed to the application when running the game.
     *
     * @return The program arguments that are passed to the application when running the game.
     */
    @Input
    @DSLProperty
    ListProperty<String> getProgramArguments();

    /**
     * Defines the JVM arguments that are passed to the JVM when running the game.
     *
     * @return The JVM arguments that are passed to the JVM when running the game.
     */
    @Input
    @DSLProperty
    ListProperty<String> getJvmArguments();

    /**
     * Indicates if this run is a single instance run.
     * If this is set to true, then no other copy of this run configuration can be started while this run configuration is running.
     *
     * @return {@code true} if this run is a single instance run; otherwise, {@code false}.
     */
    @DSLProperty
    Property<Boolean> getIsSingleInstance();

    /**
     * Defines the system properties that are passed to the JVM when running the game.
     *
     * @return The system properties that are passed to the JVM when running the game.
     */
    @DSLProperty
    MapProperty<String, String> getSystemProperties();

    /**
     * Defines the working directory that is used when running the game.
     *
     * @return The working directory that is used when running the game.
     */
    @DSLProperty
    DirectoryProperty getWorkingDirectory();

    /**
     * Indicates if this run is a client run.
     *
     * @return {@code true} if this run is a client run; otherwise, {@code false}.
     */
    @DSLProperty
    Property<Boolean> getIsClient();

    /**
     * Defines the jars that are used as a mod.
     *
     * @return The jars that are used as a mod.
     */
    @DSLProperty
    ListProperty<TaskProvider<? extends Jar>> getRunningJars();

    /**
     * Configures the run using the type with the same name.
     * Throwing an exception if no type could be found.
     */
    void configure();

    /**
     * Configures the run using the type with the specified name.
     * Throwing an exception if no type could be found.
     *
     * @param type The name of the type to use to configure the run.
     */
    void configure(@NotNull final String type);

    /**
     * Configures the run using the given type.
     *
     * @param type The type to use to configure the run.
     */
    void configure(@NotNull final Type type);
}
