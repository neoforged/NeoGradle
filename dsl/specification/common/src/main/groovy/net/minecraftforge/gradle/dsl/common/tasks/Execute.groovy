package net.minecraftforge.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.annotations.DefaultMethods
import net.minecraftforge.gradle.dsl.annotations.InternalFields
import net.minecraftforge.gradle.dsl.common.util.RegexUtils
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.JavaExecSpec

import java.util.function.Function
import java.util.regex.Matcher
import java.util.stream.Collectors

/**
 * Defines a task which can execute any java command.
 * Has a workspace, output and a java version associated.
 */
@CompileStatic
@DefaultMethods
interface Execute extends WithWorkspace, WithOutput, WithJavaVersion {


    default Provider<List<String>> applyVariableSubstitutions(Provider<List<String>> list) {
        return list.map(values -> values.stream().map(this::applyVariableSubstitutions).collect(Collectors.toList())) as Provider<List<String>>;
    }

    @TaskAction
    default void execute() throws Throwable {
        final Provider<List<String>> jvmArgs = applyVariableSubstitutions(getJvmArguments());
        final Provider<List<String>> programArgs = applyVariableSubstitutions(getRuntimeProgramArguments());

        final File outputFile = ensureFileWorkspaceReady(getOutput());
        final File logFile = ensureFileWorkspaceReady(getLogFile());
        final File consoleLogFile = ensureFileWorkspaceReady(getConsoleLogFile().get());

        final Provider<String> mainClass = getMainClass();
        final Provider<String> executable = getExecutablePath();

        final Execute me = this

        try (BufferedOutputStream log_out = new BufferedOutputStream(new FileOutputStream(consoleLogFile))) {
            getProject().javaexec({ JavaExecSpec java ->
                PrintWriter writer = new PrintWriter(log_out);
                Function<String, CharSequence> quote = s -> (CharSequence)('"' + s + '"');
                writer.println("JVM Args:          " + jvmArgs.get().stream().map(quote).collect(Collectors.joining(", ")));
                writer.println("Run Args:          " + programArgs.get().stream().map(quote).collect(Collectors.joining(", ")));
                writer.println("JVM:               " + executable.get());
                writer.println("Classpath:         " + me.getExecutingJar().get().getAsFile().getAbsolutePath());
                writer.println("Working Dir:       " + me.getOutputDirectory().get().getAsFile().getAbsolutePath());
                writer.println("Main Class:        " + mainClass.get());
                writer.println("Program log file:  " + logFile.getAbsolutePath());
                writer.println("Output file:       " + outputFile.getAbsolutePath());
                writer.flush();

                java.executable(executable.get());
                java.setJvmArgs(jvmArgs.get());
                java.setArgs(programArgs.get());
                java.setClasspath(me.getProject().files(me.getExecutingJar().get()));
                java.setWorkingDir(me.getOutputDirectory().get());
                java.getMainClass().set(mainClass);
                java.setStandardOutput(log_out);
            }).rethrowFailure().assertNormalExitValue();
        }
    }

    default String applyVariableSubstitutions(String value) {
        final Map<String, Provider<String>> runtimeArguments = getRuntimeArguments().get();
        final Map<String, File> data = getRuntimeData().get();

        Matcher matcher = RegexUtils.REPLACE_PATTERN.matcher(value);
        if (!matcher.find()) return value; // Not a replaceable string

        String argName = matcher.group(1);
        if (argName != null) {
            Provider<String> argument = runtimeArguments.get(argName);
            if (argument != null) {
                try {
                    return argument.get();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get runtime argument " + argName, e);
                }
            }

            File dataElement = data.get(argName);
            if (dataElement != null) {
                return dataElement.getAbsolutePath();
            }
        }

        throw new IllegalStateException("The string '" + value + "' did not return a valid substitution match!");
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
     * Defines the path to the jar that will be executed.
     *
     * @return The path to the jar.
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
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
    @Internal
    MapProperty<String, Provider<String>> getRuntimeArguments();
}
