package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.tasks.ForgeGradleBaseTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class ArtifactFromOutput extends ForgeGradleBaseTask {

    public ArtifactFromOutput() {
        getOutput().convention(getInput());
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
