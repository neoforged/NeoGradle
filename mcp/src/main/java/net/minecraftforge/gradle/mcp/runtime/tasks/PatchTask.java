package net.minecraftforge.gradle.mcp.runtime.tasks;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class PatchTask extends McpRuntimeTask {


    public PatchTask() {
        super();

        getPatchDirectory().fileProvider(getRuntimeData().map(data -> data.get("patches").file()));
        getRejectsFile().fileProvider(getFileInOutputDirectory("rejects.zip"));

        getInput().finalizeValueOnRead();
        getPatchDirectory().finalizeValueOnRead();
        getRejectsFile().finalizeValueOnRead();
    }

    @TaskAction
    public void run() throws Exception {
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutputFile()).get();
        final File rejects = getRejectsFile().get().getAsFile();

        CliOperation.Result<PatchOperation.PatchesSummary> result = PatchOperation.builder()
                .logTo(new LoggingOutputStream(getProject().getLogger(), LogLevel.LIFECYCLE))
                .basePath(input.toPath())
                .patchesPath(getUnpackedMcpZipDirectory().get().getAsFile().toPath())
                .patchesPrefix(getUnpackedMcpZipDirectory().get().getAsFile().toPath().relativize(getPatchDirectory().get().getAsFile().toPath()).toString())
                .outputPath(output.toPath())
                .verbose(false)
                .mode(PatchMode.OFFSET)
                .rejectsPath(rejects.toPath())
                .build()
                .operate();

        boolean success = result.exit == 0;
        if (!success) {
            getProject().getLogger().error("Rejects saved to: {}", rejects);
            throw new RuntimeException("Patch failure.");
        }
    }

    @Input
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();

    @Input
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getPatchDirectory();

    @OutputFile
    public abstract RegularFileProperty getRejectsFile();
}
