package net.minecraftforge.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
@CompileStatic
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
    @DSLProperty
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getInput();

    @OutputFile
    @DSLProperty
    abstract RegularFileProperty getOutput();

}
