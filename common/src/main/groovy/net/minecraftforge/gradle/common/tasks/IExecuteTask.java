package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaLauncher;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface IExecuteTask extends ITaskWithWorkspace, ITaskWithOutput, ITaskWithJavaVersion {

    Pattern REPLACE_PATTERN = Pattern.compile("^\\{(\\w+)}$");

    default Provider<List<String>> applyVariableSubstitutions(Provider<List<String>> list) {
        return list.map(values -> values.stream().map(this::applyVariableSubstitutions).collect(Collectors.toList()));
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

        try (BufferedOutputStream log_out = new BufferedOutputStream(new FileOutputStream(consoleLogFile))) {
            getProject().javaexec(java -> {
                PrintWriter writer = new PrintWriter(log_out);
                Function<String, String> quote = s -> '"' + s + '"';
                writer.println("JVM Args:          " + jvmArgs.get().stream().map(quote).collect(Collectors.joining(", ")));
                writer.println("Run Args:          " + programArgs.get().stream().map(quote).collect(Collectors.joining(", ")));
                writer.println("JVM:               " + executable.get());
                writer.println("Classpath:         " + getExecutingJar().get().getAsFile().getAbsolutePath());
                writer.println("Working Dir:       " + getOutputDirectory().get().getAsFile().getAbsolutePath());
                writer.println("Main Class:        " + mainClass.get());
                writer.println("Program log file:  " + logFile.getAbsolutePath());
                writer.println("Output file:       " + outputFile.getAbsolutePath());
                writer.flush();

                java.executable(executable.get());
                java.setJvmArgs(jvmArgs.get());
                java.setArgs(programArgs.get());
                java.setClasspath(getProject().files(getExecutingJar().get()));
                java.setWorkingDir(getOutputDirectory().get());
                java.getMainClass().set(mainClass);
                java.setStandardOutput(log_out);
            }).rethrowFailure().assertNormalExitValue();
        }
    }

    default String applyVariableSubstitutions(String value) {
        final Map<String, Provider<String>> runtimeArguments = getRuntimeArguments().get();
        final Map<String, File> data = getRuntimeData().get();

        Matcher matcher = REPLACE_PATTERN.matcher(value);
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


    @Input
    ListProperty<String> getJvmArguments();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    RegularFileProperty getExecutingJar();

    @Input
    Property<String> getMainClass();

    @OutputFile
    RegularFileProperty getConsoleLogFile();

    @OutputFile
    RegularFileProperty getLogFile();

    @Internal
    ListProperty<String> getRuntimeProgramArguments();

    @Internal
    Provider<String> getExecutablePath();

    /**
     * The output directory for this step, also doubles as working directory for this step.
     *
     * @return The output and working directory for this step.
     */
    @Internal
    DirectoryProperty getOutputDirectory();

    @Internal
    MapProperty<String, File> getRuntimeData();

    @Internal
    MapProperty<String, Provider<String>> getRuntimeArguments();
}
