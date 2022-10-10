package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.util.ICacheFileSelector;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class FileCacheProviding extends McpRuntime {

    @TaskAction
    public void provide() throws IOException {
        final Path output = ensureFileWorkspaceReady(getOutput()).toPath();
        final Path source = getFileCache().get().getAsFile().toPath().resolve(getSelector().get().getCacheFileName());

        if (!Files.exists(source)) {
            throw new IllegalStateException("Source file does not exist: " + source);
        }

        Files.copy(source, output);
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getFileCache();

    @Nested
    public abstract Property<ICacheFileSelector> getSelector();
}
