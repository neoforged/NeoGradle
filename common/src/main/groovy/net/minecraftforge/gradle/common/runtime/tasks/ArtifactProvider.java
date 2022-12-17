package net.minecraftforge.gradle.common.runtime.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class ArtifactProvider extends Runtime {

    @TaskAction
    public void doProvide() throws Exception {
        final Path output = ensureFileWorkspaceReady(getOutput()).toPath();
        final Path source = getInput().get().getAsFile().toPath();

        if (!Files.exists(source)) {
            throw new IllegalStateException("Source file does not exist: " + source);
        }

        Files.copy(source, output);
    }


    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutput();

}
