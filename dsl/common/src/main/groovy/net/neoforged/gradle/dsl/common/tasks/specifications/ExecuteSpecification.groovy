package net.neoforged.gradle.dsl.common.tasks.specifications

import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

interface ExecuteSpecification extends ProjectSpecification, OutputSpecification, JavaVersionSpecification {

    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, DISABLED
    }


    /**
     * Defines the jvm arguments in a list which are passed to the java executable.
     *
     * @return The jvm arguments.
     */
    @Input
    @DSLProperty
    ListProperty<String> getJvmArguments();

    /**
     * Defines the paths to the jars that will be executed.
     *
     * @return The paths to the jars.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    @DSLProperty
    ConfigurableFileCollection getExecutingClasspath();

    /**
     * Defines the main class that will be executed.
     * When this value is not supplied, it is retrieved from the manifest of the executing jar.
     *
     * @return The main class to execute.
     */
    @Input
    @DSLProperty
    Property<String> getMainClass();

    /**
     * Defines the path to the console log file.
     *
     * @return The path to the console log file.
     */
    @OutputFile
    @DSLProperty
    RegularFileProperty getConsoleLogFile();

    /**
     * Defines the path to the program log file.
     *
     * @return The path to the program log file.
     */
    @Internal
    @DSLProperty
    RegularFileProperty getLogFile();

    /**
     * Defines the interpolated arguments that will be passed to the program.
     *
     * @return The interpolated arguments for the programm.
     */
    @Internal
    ListProperty<String> getRuntimeProgramArguments();

    /**
     * Defines the path to the executable that will be used to run the program.
     * Normally this is derived from the base java version.
     *
     * @return The path to the executable.
     */
    @Internal
    Provider<String> getExecutablePath();

    /**
     * The output directory for this step, also doubles as working directory for this step.
     *
     * @return The output and working directory for this step.
     */
    @Internal
    DirectoryProperty getOutputDirectory();

    /**
     * The interpolated runtime data that will be used to interpolate the arguments.
     *
     * @return The interpolated runtime data.
     */
    @Internal
    MapProperty<String, FileTree> getRuntimeData();

    /**
     * The interpolated runtime arguments that will be used to interpolate the arguments.
     *
     * @return The interpolated runtime arguments.
     */
    @Internal
    MapProperty<String, Provider<String>> getRuntimeArguments();


    /**
     * The interpolated runtime arguments that will be used to interpolate the arguments, with multiple values
     *
     * @return The interpolated runtime arguments.
     */
    @Internal
    MapProperty<String, Provider<List<String>>> getMultiRuntimeArguments();

    @DSLProperty
    @Input
    Property<LogLevel> getLogLevel();
}
