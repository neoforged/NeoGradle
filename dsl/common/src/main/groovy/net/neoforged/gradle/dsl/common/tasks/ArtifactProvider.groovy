package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
@CompileStatic
abstract class ArtifactProvider extends NeoGradleBase implements WithOutput {

    @TaskAction
    void doProvide() throws Exception {
        final Path output = ensureFileWorkspaceReady(getOutput()).toPath();
        final Path source = getInputFiles().getSingleFile().toPath();

        if (!Files.exists(source)) {
            throw new IllegalStateException("Source file does not exist: " + source);
        }

        Files.copy(source, output);
    }

    @InputFiles
    @DSLProperty
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract ConfigurableFileCollection getInputFiles();

    @OutputFile
    @DSLProperty
    abstract RegularFileProperty getOutput();

}
