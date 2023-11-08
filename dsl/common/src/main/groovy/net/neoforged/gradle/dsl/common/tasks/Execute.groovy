package net.neoforged.gradle.dsl.common.tasks

import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DefaultMethods
import net.neoforged.gradle.dsl.common.tasks.specifications.ExecuteSpecification
import net.neoforged.gradle.dsl.common.util.RegexUtils
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
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


    default List<String> interpolateVariableSubstitution(String value, String previous) {
        final Map<String, Provider<String>> runtimeArguments = getRuntimeArguments().get()
        final Map<String, Provider<List<String>>> multiRuntimeArguments = getMultiRuntimeArguments().get()
        final Map<String, Provider<File>> data = getRuntimeData().get()

        Matcher matcher = RegexUtils.REPLACE_PATTERN.matcher(value)
        if (!matcher.find()) return Lists.newArrayList(value) // Not a replaceable string

        String argName = matcher.group(1)
        if (argName != null) {
            Provider<String> argument = runtimeArguments.get(argName)
            if (argument != null) {
                try {
                    return Lists.newArrayList(argument.get())
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get runtime argument " + argName, e)
                }
            }

            Provider<List<String>> multiArgument = multiRuntimeArguments.get(argName)
            if (multiArgument != null) {
                if (previous == null) {
                    throw new IllegalStateException("Can not substitute a multi runtime argument value without a previous key to add.")
                }

                try {
                    final List<String> values = multiArgument.get()
                    final List<String> result = new ArrayList<>()
                    for (final def v in values) {
                        result.add(previous)
                        result.add(v)
                    }
                    return result
                } catch (Exception e) {
                    throw new RuntimeException("Failed to get the multi runtime argument " + argName, e)
                }
            }


            Provider<File> dataElement = data.get(argName)
            if (dataElement != null) {
                return Lists.newArrayList(dataElement.get().getAbsolutePath())
            }
        }

        final String argKeys = String.join(", ", runtimeArguments.keySet())
        final String multiArgKeys = String.join(", ", multiRuntimeArguments.keySet())
        final String dataKeys = String.join(", ", data.keySet())
        final String error = "The string '$value' did not return a valid substitution match! Available arguments: ${argKeys}, multi arguments: ${multiArgKeys}, available data: ${dataKeys}."
        throw new IllegalStateException(error)
    }

    default Provider<List<String>> applyVariableSubstitutions(Provider<List<String>> list) {
        return list.map(values -> {
            final List<String> interpolated = new ArrayList<>()
            for (i in 0..<values.size()) {
                final String value = values.get(i)
                final String previous = i == 0 ? null : values.get(i - 1)

                final List<String> substituted = ((Execute) this).interpolateVariableSubstitution(value, previous)

                if (substituted.size() != 1) {
                    interpolated.removeAt(interpolated.size() - 1);
                }

                interpolated.addAll(substituted)
            }

            return interpolated
        }) as Provider<List<String>>
    }

    @TaskAction
    default void execute() throws Throwable {
        final Provider<List<String>> jvmArgs = applyVariableSubstitutions(getJvmArguments())
        final Provider<List<String>> programArgs = applyVariableSubstitutions(getRuntimeProgramArguments())

        final File outputFile = ensureFileWorkspaceReady(getOutput())
        final File logFile = ensureFileWorkspaceReady(getLogFile())
        final File consoleLogFile = ensureFileWorkspaceReady(getConsoleLogFile().get())

        final Provider<String> mainClass = getMainClass()
        final Provider<String> executable = getExecutablePath()

        final Execute me = this

        try (BufferedOutputStream log_out = new BufferedOutputStream(new FileOutputStream(consoleLogFile))) {
            getExecuteOperation().javaexec({ JavaExecSpec java ->
                PrintWriter writer = new PrintWriter(log_out)
                Function<String, CharSequence> quote = s -> (CharSequence)('"' + s + '"')
                writer.println("JVM Args:          " + jvmArgs.get().stream().map(quote).collect(Collectors.joining(", ")))
                writer.println("Run Args:          " + programArgs.get().stream().map(quote).collect(Collectors.joining(", ")))
                writer.println("JVM:               " + executable.get())
                writer.println("Classpath:         " + me.getExecutingJar().get().getAsFile().getAbsolutePath())
                writer.println("Working Dir:       " + me.getOutputDirectory().get().getAsFile().getAbsolutePath())
                writer.println("Main Class:        " + mainClass.get())
                writer.println("Program log file:  " + logFile.getAbsolutePath())
                writer.println("Output file:       " + outputFile.getAbsolutePath())
                writer.flush()

                java.executable(executable.get())
                java.setJvmArgs(jvmArgs.get())
                java.setArgs(programArgs.get())
                java.setClasspath(me.getObjectFactory().fileCollection().from(me.getExecutingJar().get()))
                java.setWorkingDir(me.getOutputDirectory().get())
                java.getMainClass().set(mainClass)
                java.setStandardOutput(log_out)

                java.setMaxHeapSize(String.format("%dm", Runtime.getRuntime().maxMemory().intdiv(1024 * 1024)))
            }).rethrowFailure().assertNormalExitValue()
        }
    }

}
