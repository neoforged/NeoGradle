package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.tasks.ITaskWithJavaVersion;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.tasks.JavaRuntimeTask;
import net.minecraftforge.gradle.common.util.FileWrapper;
import net.minecraftforge.gradle.mcp.util.CacheableMinecraftVersion;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@CacheableTask
public abstract class McpRuntime extends JavaRuntimeTask implements ITaskWithOutput {

    public McpRuntime() {
        super();

        //All of these tasks belong to the MCP group
        setGroup("mcp");

        //Sets up the base configuration for directories and outputs.
        getMcpDirectory().convention(getProject().getLayout().getBuildDirectory().dir("mcp"));
        getUnpackedMcpZipDirectory().convention(getMcpDirectory().dir("unpacked"));
        getStepsDirectory().convention(getMcpDirectory().dir("steps"));

        //And configure output default locations.
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
        getOutputFileName().convention(getArguments().map(arguments -> "output.%s".formatted(arguments.getOrDefault("outputExtension", "jar"))));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        //Configure the default runtime data map:
        getRuntimeArguments().convention(getArguments().map(arguments -> {
            final Map<String, Object> result = new HashMap<>(arguments);
            buildRuntimeArguments(result);
            return result;
        }));
        getRuntimeData().convention(getData().map(data -> {
            final Directory unpackedMcpDirectory = getUnpackedMcpZipDirectory().get();
            final Map<String, FileWrapper> result = new HashMap<>();
            data.forEach((key, value) -> result.put(key, new FileWrapper(unpackedMcpDirectory.file(value.file().getPath()).getAsFile())));
            return result;
        }));
    }

    @Internal
    public abstract DirectoryProperty getMcpDirectory();

    @Internal
    public abstract DirectoryProperty getUnpackedMcpZipDirectory();

    @Internal
    public abstract DirectoryProperty getStepsDirectory();

    @Input
    public abstract Property<String> getStepName();

    @Input
    public abstract Property<String> getSide();

    @Nested
    public abstract Property<CacheableMinecraftVersion> getMinecraftVersion();

    @Input
    public abstract MapProperty<String, FileWrapper> getData();

    @Input
    public abstract MapProperty<String, FileWrapper> getRuntimeData();

    @Input
    public abstract MapProperty<String, Object> getArguments();

    @Input
    public abstract MapProperty<String, Object> getRuntimeArguments();

    @Input
    public abstract Property<String> getOutputFileName();

    @Internal
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Internal
    protected Provider<File> getFileInOutputDirectory(final String fileName) {
        return getOutputDirectory().map(directory -> directory.file(fileName).getAsFile());
    }

    @Internal
    protected Provider<File> getFileInOutputDirectory(final Provider<String> fileName) {
        return getOutputDirectory().flatMap(directory -> fileName.map(f -> directory.file(f).getAsFile()));
    }

    protected void buildRuntimeArguments(final Map<String, Object> arguments) {
        arguments.computeIfAbsent("output", key -> getOutput().get().getAsFile().getAbsolutePath());
        arguments.computeIfAbsent("outputDir", key -> getOutputDirectory().get().getAsFile().getAbsolutePath());
        arguments.computeIfAbsent("outputExtension", key -> getOutputFileName().get().substring(getOutputFileName().get().lastIndexOf('.') + 1));
        arguments.computeIfAbsent("outputFileName", key -> getOutputFileName().get());
        arguments.computeIfAbsent("mcpDir", key -> getMcpDirectory().get().getAsFile().getAbsolutePath());
        arguments.computeIfAbsent("unpackedMcpZip", key -> getUnpackedMcpZipDirectory().get().getAsFile().getAbsolutePath());
        arguments.computeIfAbsent("stepsDir", key -> getStepsDirectory().get().getAsFile().getAbsolutePath());
        arguments.computeIfAbsent("stepName", key -> getStepName().get());
        arguments.computeIfAbsent("side", key -> getSide().get());
        arguments.computeIfAbsent("minecraftVersion", key -> getMinecraftVersion().get().toString());
        arguments.computeIfAbsent("javaVersion", key -> getJavaVersion().get().toString());
    }
}
