package net.minecraftforge.gradle.common.runtime.tasks;

import org.apache.commons.io.FileUtils;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class CollectDependencyLibraries extends DefaultRuntime {

    public CollectDependencyLibraries() {
        getOutputFileName().convention("libraries.txt");
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBaseLibraryFile();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getDependencyFiles();

    @TaskAction
    public void execute() throws IOException {
        final File output = ensureFileWorkspaceReady(getOutput().get().getAsFile());
        final File definitionLibraries = getBaseLibraryFile().get().getAsFile();

        FileUtils.copyFile(definitionLibraries, output);

        for (File file : getDependencyFiles()) {
            FileUtils.write(output, "\n-e=" + file.getAbsolutePath(), true);
        }
    }
}
