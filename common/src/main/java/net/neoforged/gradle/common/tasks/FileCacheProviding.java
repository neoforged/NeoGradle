package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class FileCacheProviding extends NeoGradleBase implements WithOutput, WithWorkspace {

    public FileCacheProviding() {
    }

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
    @PathSensitive(PathSensitivity.NONE)
    public abstract DirectoryProperty getFileCache();

    @Nested
    public abstract Property<CacheFileSelector> getSelector();
}
