package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.hasher.TaskHashingAware;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.dsl.common.runtime.tasks.AsPartOfStep;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.action.DownloadFileAction;
import net.neoforged.gradle.common.runtime.tasks.action.ExtractFileAction;
import net.neoforged.gradle.common.util.VersionJson;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@CacheableTask()
public abstract class ExtractNatives extends NeoGradleBase implements TaskHashingAware, WithOutput, AsPartOfStep
{

    public ExtractNatives() {
        //Sets up the base configuration for directories and outputs.
        getStepsDirectory().convention(getRuntimeDirectory().dir("steps"));
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));

        getVersionJson().convention(getVersionJsonFile().map(TransformerUtils.guard(file -> VersionJson.get(file.getAsFile()))));
        getLibrariesDirectory().convention(getOutputDirectory().map(dir -> dir.dir("libraries")));
        getIsOffline().convention(getProject().getGradle().getStartParameter().isOffline());
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCache();

    @Override
    public Map<String, Object> getHashableProperties()
    {
        var properties = new HashMap<>(getInputs().getProperties());
        properties.remove("isOffline"); //We don't care about the offline mode!
        return properties;
    }

    @TaskAction
    public void run() throws IOException
    {
        getCache().get()
            .cached(
                this,
                ICacheableJob.Initial.directory("nativesAndLibraries", getLibrariesDirectory(), this::doDownloadAndExtract)
            )
            .execute();
    }

    private File doDownloadAndExtract() {
        downloadNatives();
        extractNatives();

        final File librariesDirectory = ensureFileWorkspaceReady(getLibrariesDirectory().get().getAsFile());
        if (!librariesDirectory.exists())
            librariesDirectory.mkdirs();

        return librariesDirectory;
    }

    private File downloadNatives() {
        final VersionJson versionJson = getVersionJson().get();

        final WorkQueue executor = getWorkerExecutor().noIsolation();
        final File librariesDirectory = ensureFileWorkspaceReady(getLibrariesDirectory().get().getAsFile());

        versionJson.getNatives().forEach(library -> {
            final File outputFile = new File(librariesDirectory, library.getPath());
            executor.submit(DownloadFileAction.class, params -> {
                params.getIsOffline().set(getIsOffline());
                params.getShouldValidateHash().set(true);
                params.getOutputFile().set(outputFile);
                params.getUrl().set(library.getUrl().toString());
                params.getSha1().set(library.getSha1());
            });
        });

        executor.await();

        return librariesDirectory;
    }

    private void extractNatives() {
        final VersionJson versionJson = getVersionJson().get();

        final WorkQueue executor = getWorkerExecutor().noIsolation();
        //Because download and extract operate on the same directory, we can be sure that this already exists and does not need cleaning!
        final File librariesDirectory = getLibrariesDirectory().get().getAsFile();

        versionJson.getNatives().forEach(library -> {
            final File outputFile = new File(librariesDirectory, library.getPath());

            executor.submit(ExtractFileAction.class, params -> {
                params.getInputFile().set(outputFile);
                params.getOutputDirectory().set(getOutputDirectory());
                params.getShouldOverride().set(true);
                params.getShouldCleanTarget().set(false);
                params.getFilter().set(patternFilterable -> patternFilterable.exclude(fileTreeElement -> fileTreeElement.getPath().startsWith("META-INF")));
                params.getRenamer().set(path -> {
                    int lastPathSeparatorIndex = path.lastIndexOf('/');
                    return lastPathSeparatorIndex == -1 ? path : path.substring(lastPathSeparatorIndex);
                });
            });
        });

        executor.await();
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionJsonFile();

    @Input
    public abstract Property<VersionJson> getVersionJson();

    @OutputDirectory
    public abstract DirectoryProperty getLibrariesDirectory();

    @Input
    public abstract Property<Boolean> getIsOffline();

    @Override
    public Provider<FileTree> getOutputAsTree()
    {
        return getOutput().map(it -> getObjectFactory().fileTree().from(it));
    }
}
