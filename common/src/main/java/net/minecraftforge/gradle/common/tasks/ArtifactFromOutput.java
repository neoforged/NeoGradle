package net.minecraftforge.gradle.common.tasks;

import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class ArtifactFromOutput extends ForgeGradleBaseTask implements ITaskWithOutput {

    public ArtifactFromOutput() {
        getOutputFileName().convention(getName() + ".jar");
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir("artifacts").flatMap(directory -> directory.file(getOutputFileName())));
    }

    @TaskAction
    public void doCopy() throws Exception {
        FileUtils.copyFile(getInput().getAsFile().get(), getOutput().getAsFile().get());
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
