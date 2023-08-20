package net.neoforged.gradle.common.tasks;

import com.google.common.io.Files;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

public abstract class RawAndSourceCombiner extends NeoGradleBase implements WithOutput {

    @TaskAction
    public void doCombine() {
        final File rawJarOutput = ensureFileWorkspaceReady(getOutput());
        final File sourceJarOutput = ensureFileWorkspaceReady(getSourceJarOutput());

        final File rawJarInput = getInput().getAsFile().get();
        final File sourceJarInput = getSourceJarInput().getAsFile().get();

        copy(rawJarInput, rawJarOutput);
        copy(sourceJarInput, sourceJarOutput);
    }

    private static void copy(final File input, final File output) {
        try {
            Files.copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy: %s to output: %s", input.getAbsolutePath(), output.getAbsolutePath()), e);
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getSourceJarInput();

    @OutputFile
    public abstract RegularFileProperty getSourceJarOutput();
}
