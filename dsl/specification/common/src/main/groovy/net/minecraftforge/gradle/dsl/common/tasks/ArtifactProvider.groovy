package net.minecraftforge.gradle.dsl.common.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
abstract class ArtifactProvider extends ForgeGradleBase implements WithOutput {

    @TaskAction
    void doProvide() throws Exception {
        final Path output = ensureFileWorkspaceReady(getOutput()).toPath();
        final Path source = getInput().get().getAsFile().toPath();

        if (!Files.exists(source)) {
            throw new IllegalStateException("Source file does not exist: " + source);
        }

        Files.copy(source, output);
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getInput();

    @OutputFile
    abstract RegularFileProperty getOutput();

}
