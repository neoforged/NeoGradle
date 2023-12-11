package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CacheKey;
import net.neoforged.gradle.common.caching.SharedCacheService;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.tasks.TaskExecutionOutcome;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@CacheableTask
public abstract class Execute extends DefaultRuntime implements net.neoforged.gradle.dsl.common.tasks.Execute {

    public Execute() {
        super();
        
        getLogFileName().convention(getArguments().getOrDefault("log", getProviderFactory().provider(() -> "log.log")).orElse("log.log"));
        getLogFile().convention(getOutputDirectory().flatMap(d -> getLogFileName().map(d::file)));

        getConsoleLogFileName().convention(getArguments().getOrDefault("console.log", getProviderFactory().provider(() -> "console.log")));
        getConsoleLogFile().convention(getOutputDirectory().flatMap(d -> getConsoleLogFileName().map(d::file)));

        getRuntimeProgramArguments().convention(getProgramArguments());
        getMultiRuntimeArguments().convention(getMultiArguments().AsMap());
    }

    @TaskAction
    public void execute() throws Throwable {
        List<String> jvmArgs = applyVariableSubstitutions(getJvmArguments()).get();
        List<String> programArgs = applyVariableSubstitutions(getRuntimeProgramArguments()).get();

        File outputFile = ensureFileWorkspaceReady(getOutput());
        File logFile = ensureFileWorkspaceReady(getLogFile());
        File consoleLogFile = ensureFileWorkspaceReady(getConsoleLogFile().get());

        RegularFile executableJar = getExecutingJar().get();

        SharedCacheService cachingService = getSharedCacheService().get();
        CacheKey cacheKey = cachingService.cacheKeyBuilder(getProject())
                // We use the NeoForm step name as the cache domain to make debugging the cache state easier
                // since every step will have its own cache directory.
                .cacheDomain(getStepName().get())
                .tool(executableJar.getAsFile().toPath())
                .inputFiles(getInputs().getFiles().getFiles())
                .arguments(programArgs)
                .build();

        boolean usedCache = cachingService.cacheOutput(getProject(), cacheKey, outputFile.toPath(), () -> {
            Provider<String> executable = getExecutablePath();

            try (BufferedOutputStream log_out = new BufferedOutputStream(new FileOutputStream(consoleLogFile))) {
                getExecuteOperation().javaexec((JavaExecSpec java) -> {
                    PrintWriter writer = new PrintWriter(log_out);
                    Function<String, CharSequence> quote = s -> (CharSequence) ('"' + s + '"');
                    writer.println("JVM Args:          " + jvmArgs.stream().map(quote).collect(Collectors.joining(", ")));
                    writer.println("Run Args:          " + programArgs.stream().map(quote).collect(Collectors.joining(", ")));
                    writer.println("JVM:               " + executable.get());
                    writer.println("Executable Jar:    " + executableJar.getAsFile().getAbsolutePath());
                    writer.println("Working Dir:       " + getOutputDirectory().get().getAsFile().getAbsolutePath());
                    writer.println("Program log file:  " + logFile.getAbsolutePath());
                    writer.println("Output file:       " + outputFile.getAbsolutePath());
                    writer.flush();

                    java.executable(executable.get());
                    java.setJvmArgs(jvmArgs);
                    java.setArgs(programArgs);
                    java.setClasspath(getObjectFactory().fileCollection().from(executableJar));
                    java.setWorkingDir(getOutputDirectory().get());
                    java.setStandardOutput(log_out);
                }).rethrowFailure().assertNormalExitValue();
            }
        });

        if (usedCache) {
            setDidWork(false);
        }
    }

    @ServiceReference(CommonProjectPlugin.NEOFORM_CACHE_SERVICE)
    protected abstract Property<SharedCacheService> getSharedCacheService();

    @Input
    public abstract Property<String> getConsoleLogFileName();

    @Input
    public abstract Property<String> getLogFileName();

    @Input
    public abstract ListProperty<String> getProgramArguments();

    @Override
    public void buildRuntimeArguments(Map<String, Provider<String>> arguments) {
        super.buildRuntimeArguments(arguments);
        arguments.computeIfAbsent("log", k -> newProvider(getLogFile().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("console.log", k -> newProvider(getConsoleLogFile().get().getAsFile().getAbsolutePath()));
    }
}
