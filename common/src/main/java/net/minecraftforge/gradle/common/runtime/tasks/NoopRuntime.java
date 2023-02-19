package net.minecraftforge.gradle.common.runtime.tasks;

import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class NoopRuntime extends DefaultRuntime {

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
    public abstract RegularFileProperty getInput();
}
