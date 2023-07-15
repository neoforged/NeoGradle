package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DefaultMethods
import net.neoforged.gradle.dsl.common.util.RegexUtils
import net.neoforged.gradle.dsl.common.tasks.specifications.ExecuteSpecification
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
interface Execute extends WithWorkspace, WithOutput, WithJavaVersion, ExecuteSpecification {


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

        final String argKeys = String.join(", ", runtimeArguments.keySet());
        final String dataKeys = String.join(", ", data.keySet());
        final String error = "The string '$value' did not return a valid substitution match! Available arguments: ${argKeys}, available data: ${dataKeys}."
        throw new IllegalStateException(error);
    }
}
