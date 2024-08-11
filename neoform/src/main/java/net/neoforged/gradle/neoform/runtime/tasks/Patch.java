package net.neoforged.gradle.neoform.runtime.tasks;

import io.codechicken.diffpatch.cli.CliOperation;
import io.codechicken.diffpatch.cli.PatchOperation;
import io.codechicken.diffpatch.util.Input.MultiInput;
import io.codechicken.diffpatch.util.Output.MultiOutput;
import io.codechicken.diffpatch.util.PatchMode;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@CacheableTask
public abstract class Patch extends DefaultRuntime {


    public Patch() {
        super();

        getRejectsFile().fileProvider(getFileInOutputDirectory("rejects.zip"));
        getIsVerbose().convention(false);
    }


    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void run() throws Throwable {
        getCacheService().get().cached(
                this,
                    ICacheableJob.Default.file(getOutput(), this::doRun)
                ).execute();
    }

    private void doRun() throws Exception {
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutput());
        final File rejects = getRejectsFile().get().getAsFile();

        final String patchDirectory = getPatchDirectory().get();
        final ExtractingAndRootCollectingVisitor patchArchiveLocator = new ExtractingAndRootCollectingVisitor(patchDirectory);
        getPatchArchive()
                .getAsFileTree()
                .matching(filterable -> filterable.include(
                        fileTreeElement -> {
                            final String path = fileTreeElement.getPath();
                            if (patchDirectory.startsWith(path))
                                return true;

                            return (fileTreeElement.getPath() + "/").startsWith(patchDirectory);
                        } //NeoForm: Added trailing slash because has this in the data block.
                ))
                .visit(patchArchiveLocator);
        if (patchArchiveLocator.directory == null) {
            throw new RuntimeException("Patch directory not found.");
        }

        PatchOperation.Builder builder = PatchOperation.builder()
                .logTo(getLogger()::lifecycle)
                .baseInput(MultiInput.detectedArchive(input.toPath()))
                .patchesInput(MultiInput.folder(patchArchiveLocator.directory.toPath()))
                .patchedOutput(MultiOutput.detectedArchive(output.toPath()))
                .rejectsOutput(MultiOutput.detectedArchive(rejects.toPath()))
                .level(getIsVerbose().get() ? io.codechicken.diffpatch.util.LogLevel.ALL : io.codechicken.diffpatch.util.LogLevel.WARN)
                .mode(PatchMode.OFFSET);

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

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getPatchArchive();

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

    private static final class ExtractingAndRootCollectingVisitor implements FileVisitor {

        private final String filter;
        private File directory;

        private ExtractingAndRootCollectingVisitor(String filter) {
            this.filter = filter;
        }

        @Override
        public void visitDir(@NotNull FileVisitDetails dirDetails) {
            if (directory == null && (dirDetails.getRelativePath().getPathString() + "/").startsWith(filter)) {
                directory = dirDetails.getFile();
            }


            //Force the extraction.
            dirDetails.getFile();
        }

        @Override
        public void visitFile(@NotNull FileVisitDetails fileDetails) {
            //Force the extraction.
            fileDetails.getFile();
        }
    }
}
