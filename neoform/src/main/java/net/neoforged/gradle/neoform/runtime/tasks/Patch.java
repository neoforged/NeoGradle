package net.neoforged.gradle.neoform.runtime.tasks;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CentralCacheService;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.*;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
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


    @ServiceReference(CommonProjectPlugin.EXECUTE_SERVICE)
    public abstract Property<CentralCacheService> getCacheService();

    @TaskAction
    public void run() throws Throwable {
        getCacheService().get().doCached(this, this::doRun, getOutput());
    }

    public File doRun() throws Exception {
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
                .logTo(new LoggingOutputStream(getLogger(), LogLevel.LIFECYCLE))
                .basePath(input.toPath())
                .patchesPath(patchArchiveLocator.directory.toPath())
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

        return output;
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
