package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.tasks.JavaRuntimeTask;
import net.minecraftforge.gradle.common.util.FileWrapper;
import net.minecraftforge.gradle.common.util.TransformerUtils;
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

@SuppressWarnings("ResultOfMethodCallIgnored")
@CacheableTask
public abstract class McpRuntimeTask extends JavaRuntimeTask {

    public McpRuntimeTask() {
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
        getOutputFile().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

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

        //Lock all properties down once execution has started!
        getMcpDirectory().finalizeValueOnRead();
        getUnpackedMcpZipDirectory().finalizeValueOnRead();
        getStepsDirectory().finalizeValueOnRead();
        getStepName().finalizeValueOnRead();
        getSide().finalizeValueOnRead();
        getMinecraftVersion().finalizeValueOnRead();
        getData().finalizeValueOnRead();
        getRuntimeData().finalizeValueOnRead();
        getArguments().finalizeValueOnRead();
        getRuntimeArguments().finalizeValueOnRead();
        getOutputFileName().finalizeValueOnRead();
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getMcpDirectory();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getUnpackedMcpZipDirectory();

    @InputDirectory
    public abstract DirectoryProperty getStepsDirectory();

    @Input
    public abstract Property<String> getStepName();

    @Input
    public abstract Property<String> getSide();

    @Input
    @Nested
    public abstract Property<CacheableMinecraftVersion> getMinecraftVersion();

    @Input
    @Nested
    public abstract MapProperty<String, FileWrapper> getData();

    @Input
    @Nested
    public abstract MapProperty<String, FileWrapper> getRuntimeData();

    @Input
    @Nested
    public abstract MapProperty<String, Object> getArguments();

    @Input
    public abstract MapProperty<String, Object> getRuntimeArguments();

    @Input
    public abstract Property<String> getOutputFileName();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Internal
    protected Provider<File> getFileInOutputDirectory(final String fileName) {
        return getOutputDirectory().map(directory -> directory.file(fileName).getAsFile());
    }

    @Internal
    protected Provider<File> getFileInOutputDirectory(final Provider<String> fileName) {
        return getOutputDirectory().flatMap(directory -> fileName.map(f -> directory.file(f).getAsFile()));
    }

    protected void buildRuntimeArguments(final Map<String, Object> arguments) {
        arguments.computeIfAbsent("output", key -> getOutputFile().get().getAsFile().getAbsolutePath());
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

    protected Provider<File> ensureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    protected Provider<File> ensureFileWorkspaceReady(final Provider<File> fileProvider) {
        return fileProvider.map(TransformerUtils.guard(
                f -> {
                    if (f.exists()) {
                        f.delete();
                        return f;
                    }

                    f.getParentFile().mkdirs();
                    return f;
                }
        ));
    }
}
