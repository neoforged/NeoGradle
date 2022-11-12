package net.minecraftforge.gradle.common.runtime.tasks;

import net.minecraftforge.gradle.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CacheableTask
public abstract class Execute extends Runtime {

    private static final Pattern REPLACE_PATTERN = Pattern.compile("^\\{(\\w+)}$");

    public Execute() {
        super();

        getLogFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("log", getProject().provider(() -> "log.log"))));
        getLogFile().convention(getOutputDirectory().flatMap(d -> getLogFileName().map(d::file)));

        getConsoleLogFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("console.log", getProject().provider(() -> "console.log"))));
        getConsoleLogFile().convention(getOutputDirectory().flatMap(d -> getConsoleLogFileName().map(d::file)));

        getMainClass().convention(getExecutingJar().map(TransformerUtils.guardWithResource(
                jarFile -> jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS),
                f -> new JarFile(f.getAsFile())
        )));

        getExecutingJar().fileProvider(getExecutingArtifact().flatMap(artifact -> getDownloader().flatMap(downloader -> downloader.gradle(artifact, false))));

        getRuntimeProgramArguments().convention(getProgramArguments());
    }

    @TaskAction
    public void execute() throws Throwable {
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

    private Provider<List<String>> applyVariableSubstitutions(Provider<List<String>> list) {
        return list.map(values -> values.stream().map(this::applyVariableSubstitutions).collect(Collectors.toList()));
    }

    private String applyVariableSubstitutions(String value) {
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
    public abstract Property<String> getConsoleLogFileName();

    @Input
    public abstract Property<String> getLogFileName();

    @Input
    public abstract ListProperty<String> getJvmArguments();

    @Input
    public abstract ListProperty<String> getProgramArguments();

    @Input
    public abstract Property<String> getExecutingArtifact();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getExecutingJar();

    @Input
    public abstract Property<String> getMainClass();

    @OutputFile
    public abstract RegularFileProperty getConsoleLogFile();

    @OutputFile
    public abstract RegularFileProperty getLogFile();

    @Internal
    public abstract ListProperty<String> getRuntimeProgramArguments();

    @Override
    public void buildRuntimeArguments(Map<String, Provider<String>> arguments) {
        super.buildRuntimeArguments(arguments);
        arguments.computeIfAbsent("log", k -> newProvider(getLogFile().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("console.log", k -> newProvider(getConsoleLogFile().get().getAsFile().getAbsolutePath()));
    }
}
