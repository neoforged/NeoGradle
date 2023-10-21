package net.neoforged.gradle.dsl.common.tasks.specifications

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

interface ExecuteSpecification extends ProjectSpecification, OutputSpecification, JavaVersionSpecification {

    /**
     * Defines the jvm arguments in a list which are passed to the java executable.
     *
     * @return The jvm arguments.
     */
    @Input
    @DSLProperty
    ListProperty<String> getJvmArguments();

    /**
     * Defines the path to the jar that will be executed.
     *
     * @return The path to the jar.
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    @DSLProperty
    RegularFileProperty getExecutingJar();

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
    @OutputFile
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
    MapProperty<String, File> getRuntimeData();

    /**
     * The interpolated runtime arguments that will be used to interpolate the arguments.
     *
     * @return The interpolated runtime arguments.
     */
    @Input
    @PathSensitive(PathSensitivity.NONE)
    MapProperty<String, Provider<String>> getRuntimeArguments();


    /**
     * The interpolated runtime arguments that will be used to interpolate the arguments, with multiple values
     *
     * @return The interpolated runtime arguments.
     */
    @Input
    @PathSensitive(PathSensitivity.NONE)
    MapProperty<String, Provider<List<String>>> getMultiRuntimeArguments();
}