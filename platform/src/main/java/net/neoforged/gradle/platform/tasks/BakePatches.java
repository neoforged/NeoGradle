package net.neoforged.gradle.platform.tasks;

import io.codechicken.diffpatch.cli.PatchOperation;
import io.codechicken.diffpatch.util.Input.MultiInput;
import io.codechicken.diffpatch.util.Output.MultiOutput;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class BakePatches extends DefaultRuntime implements WithOutput, WithWorkspace {

    public BakePatches() {
        getLineEndings().convention(System.lineSeparator());
    }

    @TaskAction
    public void doTask() throws Exception {
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutput());

        PatchOperation.bakePatches(
                MultiInput.detectedArchive(input.toPath()),
                MultiOutput.detectedArchive(output.toPath()),
                getLineEndings().get()
        );
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<String> getLineEndings();
}
