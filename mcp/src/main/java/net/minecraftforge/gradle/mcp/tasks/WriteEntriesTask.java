package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.tasks.ForgeGradleBaseTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

@CacheableTask
public abstract class WriteEntriesTask extends ForgeGradleBaseTask {

    public WriteEntriesTask() {
        super();

        getEntries().convention(Collections.emptyList());
        getOutputFile().convention(getProject().getLayout().getBuildDirectory().dir("declaredAccessTransformers")
                .map(directory -> directory.file(getName())));
    }

    @Input
    public abstract ListProperty<String> getEntries();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void run() throws Exception {
        final File output = ensureFileWorkspaceReady(getOutputFile());
        Files.write(output.toPath(), getEntries().getOrElse(Collections.emptyList()), StandardOpenOption.CREATE_NEW);
    }
}
