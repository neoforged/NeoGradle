package net.minecraftforge.gradle.common.runtime.tasks;

import net.minecraftforge.gradle.common.tasks.JavaRuntimeTask;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@CacheableTask
public abstract class Runtime extends JavaRuntimeTask implements IRuntimeTask {

    public Runtime() {
        super();

        //All of these taskOutputs belong to the MCP group
        setGroup("mcp");

        //Sets up the base configuration for directories and outputs.
        getRuntimeDirectory().convention(getProject().getLayout().getBuildDirectory().dir("mcp"));
        getUnpackedMcpZipDirectory().convention(getRuntimeDirectory().dir("unpacked"));
        getStepsDirectory().convention(getRuntimeDirectory().dir("steps"));

        //And configure output default locations.
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
        getOutputFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("outputExtension", getProject().provider(() -> "jar")).map(extension -> String.format("output.%s", extension))));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        //Configure the default runtime data map:
        getRuntimeArguments().convention(getArguments().map(arguments -> {
            final Map<String, Provider<String>> result = new HashMap<>(arguments);
            buildRuntimeArguments(result);
            return result;
        }));
        getRuntimeData().convention(getData().map(data -> {
            final Directory unpackedMcpDirectory = getUnpackedMcpZipDirectory().get();
            final Map<String, File> result = new HashMap<>();
            data.forEach((key, value) -> result.put(key, unpackedMcpDirectory.file(value.getPath()).getAsFile()));
            return result;
        }));
    }

    protected Provider<File> getFileInOutputDirectory(final String fileName) {
        return getOutputDirectory().map(directory -> directory.file(fileName).getAsFile());
    }

    protected Provider<File> getFileInOutputDirectory(final Provider<String> fileName) {
        return getOutputDirectory().flatMap(directory -> fileName.map(f -> directory.file(f).getAsFile()));
    }

    @Internal
    public abstract MapProperty<String, File> getRuntimeData();

    @Internal
    public abstract MapProperty<String, Provider<String>> getRuntimeArguments();

    protected void buildRuntimeArguments(final Map<String, Provider<String>> arguments) {
        arguments.computeIfAbsent("output", key -> newProvider(getOutput().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("outputDir", key -> newProvider(getOutputDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("outputExtension", key -> newProvider(getOutputFileName().get().substring(getOutputFileName().get().lastIndexOf('.') + 1)));
        arguments.computeIfAbsent("outputFileName", key -> newProvider(getOutputFileName().get()));
        arguments.computeIfAbsent("mcpDir", key -> newProvider(getRuntimeDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("unpackedMcpZip", key -> newProvider(getUnpackedMcpZipDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("stepsDir", key -> newProvider(getStepsDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("stepName", key -> getStepName());
        arguments.computeIfAbsent("side", key -> getDistribution().map(ArtifactSide::getName));
        arguments.computeIfAbsent("minecraftVersion", key -> getMinecraftVersion().map(Object::toString));
        arguments.computeIfAbsent("javaVersion", key -> getRuntimeJavaLauncher().map(launcher -> launcher.getMetadata().getLanguageVersion().toString()));
    }
}
