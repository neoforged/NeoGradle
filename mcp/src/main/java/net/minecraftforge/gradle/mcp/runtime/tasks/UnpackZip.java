package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.util.CopyingFileTreeVisitor;
import net.minecraftforge.gradle.util.FileUtils;
import net.minecraftforge.gradle.common.runtime.tasks.DefaultRuntime;
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
    public void doTask() throws IOException {
        final File output = ensureFileWorkspaceReady(getUnpackingTarget().getAsFile().get());
        final File input = getInputZip().getAsFile().get();

        final FileTree source = getProject().zipTree(input);
        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output);
        source.visit(visitor);
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputZip();

    @OutputDirectory
    public abstract DirectoryProperty getUnpackingTarget();
}
