package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "a simple file-copy is not worthwhile to cache")
public abstract class ArtifactFromOutput extends NeoGradleBase implements WithOutput {

    public ArtifactFromOutput() {
        getOutputFileName().convention(getName() + ".jar");
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir("artifacts").flatMap(directory -> directory.file(getOutputFileName())));
    }

    @TaskAction
    public void doCopy() throws Exception {
        FileUtils.copyFile(getInput().getAsFile().get(), getOutput().getAsFile().get(), true);
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
