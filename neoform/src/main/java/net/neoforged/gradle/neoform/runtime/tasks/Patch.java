package net.neoforged.gradle.neoform.runtime.tasks;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import codechicken.diffpatch.util.archiver.ArchiveFormat;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class Patch extends DefaultRuntime {


    public Patch() {
        super();

        getRejectsFile().fileProvider(getFileInOutputDirectory("rejects.zip"));
        getIsVerbose().convention(false);
    }

    @TaskAction
    public void run() throws Exception {
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutput());
        final File rejects = getRejectsFile().get().getAsFile();
        final File patchArchive = getPatchArchive().get().getAsFile();

        PatchOperation.Builder builder = PatchOperation.builder()
                .logTo(new LoggingOutputStream(getLogger(), LogLevel.LIFECYCLE))
                .basePath(input.toPath())
                .patchesPath(patchArchive.toPath(), ArchiveFormat.ZIP)
                .patchesPrefix(getPatchDirectory().get())
                .outputPath(output.toPath())
                .level(getIsVerbose().get() ? codechicken.diffpatch.util.LogLevel.ALL : codechicken.diffpatch.util.LogLevel.WARN)
                .mode(PatchMode.OFFSET)
                .rejectsPath(rejects.toPath());

        if (getPatchesModifiedPrefix().isPresent()) {
            builder = builder.bPrefix(getPatchesModifiedPrefix().get());
        }

        if (getPatchesOriginalPrefix().isPresent()) {
            builder = builder.aPrefix(getPatchesOriginalPrefix().get());
        }

        CliOperation.Result<PatchOperation.PatchesSummary> result = builder.build().operate();

        boolean success = result.exit == 0;
        if (!success) {
            getProject().getLogger().error("Rejects saved to: {}", rejects);
            throw new RuntimeException("Patch failure.");
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPatchArchive();

    @Input
    public abstract Property<String> getPatchDirectory();

    @OutputFile
    public abstract RegularFileProperty getRejectsFile();

    @Input
    public abstract Property<Boolean> getIsVerbose();

    @Input
    @Optional
    public abstract Property<String> getPatchesOriginalPrefix();

    @Input
    @Optional
    public abstract Property<String> getPatchesModifiedPrefix();
}
