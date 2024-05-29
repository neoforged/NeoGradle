package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class UnpackZip extends DefaultRuntime {

    public UnpackZip() {
        getUnpackingTarget().convention(getOutputDirectory().map(dir -> dir.dir("unpacked")));
    }

    @TaskAction
    public void doTask() {
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
