package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

@CacheableTask
@CompileStatic
abstract class ArtifactProvider extends NeoGradleBase implements WithOutput {

    ArtifactProvider() {
        getContextId().convention(getOutputFileName())
    }

    @TaskAction
    void doProvide() throws Exception {
        final Path output = ensureFileWorkspaceReady(getOutput()).toPath();

        final File inputFile;
        try {
            inputFile = getInputFiles().getSingleFile()
        } catch (final IllegalStateException e) {
            throw new InvalidUserDataException("There where either none or multiple input files provided. " + getInputFiles().files
                    .stream().map { file -> file.absolutePath }.collect(Collectors.joining()), e)
        }

        final Path source = inputFile.toPath();

        if (!Files.exists(source)) {
            throw new IllegalStateException("Source file does not exist: " + source);
        }

        Files.copy(source, output);
    }

    @InputFiles
    @DSLProperty
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract ConfigurableFileCollection getInputFiles();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getContextId();
}
