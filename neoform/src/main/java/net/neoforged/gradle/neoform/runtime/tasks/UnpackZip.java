package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class UnpackZip extends DefaultRuntime {

    public UnpackZip() {
        getUnpackingTarget().convention(getOutputDirectory().map(dir -> dir.dir("unpacked")));
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void execute() throws IOException {
        getCacheService().get()
                        .cached(
                                this,
                                ICacheableJob.Default.directory(getUnpackingTarget(), this::doTask)
                        ).execute();
    }

    private void doTask() {
        final File output = ensureFileWorkspaceReady(getUnpackingTarget().getAsFile().get());
        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output);
        getInput().getAsFileTree().visit(visitor);
    }

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInput();

    @OutputDirectory
    public abstract DirectoryProperty getUnpackingTarget();
}
