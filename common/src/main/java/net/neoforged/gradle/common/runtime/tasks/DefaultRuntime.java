package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.tasks.JavaRuntimeTask;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeMultiArguments;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@CacheableTask
public abstract class DefaultRuntime extends JavaRuntimeTask implements Runtime {
    
    private final RuntimeArguments arguments;
    private final RuntimeMultiArguments multiArguments;
    
    public DefaultRuntime() {
        super();

        arguments = getObjectFactory().newInstance(RuntimeArgumentsImpl.class, getProviderFactory());
        multiArguments = getObjectFactory().newInstance(RuntimeMultiArgumentsImpl.class, getProviderFactory());
        
        //All of these taskOutputs belong to the MCP group
        setGroup("NeoGradle/runtimes");

        //Sets up the base configuration for directories and outputs.
        getStepsDirectory().convention(getRuntimeDirectory().dir("steps"));

        //And configure output default locations.
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
        getOutputFileName().convention(getArguments().getOrDefault("outputExtension", getProviderFactory().provider(() -> "jar")).map(extension -> String.format("output.%s", extension)).orElse("output.jar"));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().orElse("output.jar").map(d::file)));

        //Configure the default runtime data map:
        getRuntimeArguments().convention(getArguments().asMap().map(arguments -> {
            final Map<String, Provider<String>> result = new HashMap<>(arguments);
            buildRuntimeArguments(result);
            return result;
        }));
        getRuntimeData().convention(getSymbolicDataSources().map(dataSources -> dataSources.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> getNeoFormArchive()
                        .getAsFileTree()
                        .matching(archive -> archive.include(entry.getValue()))
        ))));
        
        getOutputDirectory().finalizeValueOnRead();
    }

    @Override
    @Nested
    public RuntimeArguments getArguments() {
        return arguments;
    }
    
    @Override
    @Nested
    public RuntimeMultiArguments getMultiArguments() {
        return multiArguments;
    }
    
    @Override
    public String getGroup() {
        final String name = getRuntimeName().getOrElse("unknown");
        return String.format("NeoGradle/Runtime/%s", name);
    }


    protected Provider<File> getFileInOutputDirectory(final String fileName) {
        return getOutputDirectory().map(directory -> directory.file(fileName).getAsFile());
    }

    protected Provider<File> getFileInOutputDirectory(final Provider<String> fileName) {
        return getOutputDirectory().flatMap(directory -> fileName.map(f -> directory.file(f).getAsFile()));
    }

    protected Provider<RegularFile> getRegularFileInOutputDirectory(final Provider<String> fileName) {
        return getOutputDirectory().flatMap(directory -> fileName.map(directory::file));
    }

    @Internal
    public abstract MapProperty<String, FileTree> getRuntimeData();

    @Input
    public abstract MapProperty<String, Provider<String>> getRuntimeArguments();

    protected void buildRuntimeArguments(final Map<String, Provider<String>> arguments) {
        arguments.computeIfAbsent("output", key -> newProvider(getOutput().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("outputDir", key -> newProvider(getOutputDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("outputExtension", key -> newProvider(getOutputFileName().get().substring(getOutputFileName().get().lastIndexOf('.') + 1)));
        arguments.computeIfAbsent("outputFileName", key -> newProvider(getOutputFileName().get()));
        arguments.computeIfAbsent("stepsDir", key -> newProvider(getStepsDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("stepName", key -> getStepName());
        arguments.computeIfAbsent("side", key -> getDistribution().map(DistributionType::getName));
        arguments.computeIfAbsent("minecraftVersion", key -> getMinecraftVersion().map(Object::toString));
        arguments.computeIfAbsent("javaVersion", key -> getJavaLauncher().map(launcher -> launcher.getMetadata().getLanguageVersion().toString()));
    }
}
