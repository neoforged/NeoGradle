package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.action.DownloadFileAction;
import net.neoforged.gradle.common.runtime.tasks.action.ExtractFileAction;
import net.neoforged.gradle.common.util.VersionJson;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;

public abstract class ExtractNatives extends DefaultRuntime {

    public ExtractNatives() {
        getVersionJson().convention(getVersionJsonFile().map(TransformerUtils.guard(file -> VersionJson.get(file.getAsFile()))));
        getLibrariesDirectory().convention(getOutputDirectory().map(dir -> dir.dir("libraries")));
    }

    @TaskAction
    public void extract() {
        downloadNatives();
        extractNatives();
    }

    private void downloadNatives() {
        final VersionJson versionJson = getVersionJson().get();

        final WorkQueue executor = getWorkerExecutor().noIsolation();
        final File librariesDirectory = ensureFileWorkspaceReady(getLibrariesDirectory().get().getAsFile());

        versionJson.getNatives().forEach(library -> {
            final File outputFile = new File(librariesDirectory, library.getPath());
            executor.submit(DownloadFileAction.class, params -> {
                params.getIsOffline().set(getProject().getGradle().getStartParameter().isOffline());
                params.getShouldValidateHash().set(true);
                params.getOutputFile().set(outputFile);
                params.getUrl().set(library.getUrl().toString());
                params.getSha1().set(library.getSha1());
            });
        });

        executor.await();
    }

    private void extractNatives() {
        final VersionJson versionJson = getVersionJson().get();

        final WorkQueue executor = getWorkerExecutor().noIsolation();
        final File librariesDirectory = ensureFileWorkspaceReady(getLibrariesDirectory().get().getAsFile());

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
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getVersionJsonFile();

    @Input
    public abstract Property<VersionJson> getVersionJson();

    @OutputDirectory
    public abstract DirectoryProperty getLibrariesDirectory();
}
