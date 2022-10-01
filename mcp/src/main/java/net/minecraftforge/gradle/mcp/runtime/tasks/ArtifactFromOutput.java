package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.tasks.ForgeGradleBaseTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

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
