package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@CacheableTask
public abstract class Download extends DefaultRuntime {

    @TaskAction
    public void doDownload() throws IOException {
        final File output = ensureFileWorkspaceReady(getOutput());
        Files.copy(getInput().getFiles().iterator().next().toPath(), output.toPath());
    }
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInput();
}
