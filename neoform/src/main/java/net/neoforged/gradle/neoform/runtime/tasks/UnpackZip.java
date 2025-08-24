package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@CacheableTask
public abstract class UnpackZip extends DefaultRuntime {

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void execute(InputChanges inputChanges) throws IOException {
        getCacheService().get()
                        .cached(
                                this,
                                ICacheableJob.Default.directory(getOutputDirectory(), () -> doTask(inputChanges))
                        ).execute();
    }

    private void doTask(InputChanges inputChanges) throws IOException
    {
        final File output = getOutputDirectory().get().getAsFile();
        if (!output.exists())
            output.mkdirs();

        if (!inputChanges.isIncremental()) {
            final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output);
            getInput().getAsFileTree().visit(visitor);

            return;
        }

        for (FileChange fileChange : inputChanges.getFileChanges(getInput()))
        {
            if (fileChange.getChangeType() == ChangeType.REMOVED)
            {
                final File outputFile = new File(output, fileChange.getNormalizedPath());
                if (outputFile.exists())
                    outputFile.delete();

                continue;
            }

            final File outputFile = new File(output, fileChange.getNormalizedPath());
            Files.copy(fileChange.getFile().toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInput();



    @Override
    public Provider<FileTree> getOutputAsTree()
    {
        //Walk the outputs dir
        return getOutputDirectory().map(it -> getObjectFactory().fileTree().from(it));
    }
}
