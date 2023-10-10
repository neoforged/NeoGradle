package net.neoforged.gradle.platform.tasks;

import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.InputPath;
import codechicken.diffpatch.util.OutputPath;
import codechicken.diffpatch.util.archiver.ArchiveFormat;
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

        final ArchiveFormat inputFormat = ArchiveFormat.findFormat(input.toPath());
        final ArchiveFormat outputFormat = ArchiveFormat.findFormat(output.toPath());

        PatchOperation.bakePatches(
                new InputPath.FilePath(input.toPath(), inputFormat),
                new OutputPath.FilePath(output.toPath(), outputFormat),
                getLineEndings().get()
        );
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<String> getLineEndings();
}
