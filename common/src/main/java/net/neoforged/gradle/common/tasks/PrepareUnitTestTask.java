package net.neoforged.gradle.common.tasks;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@CacheableTask
public abstract class PrepareUnitTestTask implements Task {

    public void execute() throws Exception {
        final File output = getProgramArgumentsFile().get().getAsFile();

        if (!output.getParentFile().exists()) {
            if (!output.getParentFile().mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + output.getParentFile());
            }
        }
        try {
            Files.write(output.toPath(), getProgramArguments().get(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getProgramArgumentsFile();

    @Input
    public abstract ListProperty<String> getProgramArguments();

}
