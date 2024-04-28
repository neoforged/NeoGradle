package net.neoforged.gradle.userdev.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

@CacheableTask
public abstract class ClasspathSerializer extends DefaultRuntime {

    public ClasspathSerializer() {
        getOutputFileName().convention("classpath.txt");

        setGroup("NeoGradle/Runs");
        setDescription("Serializes the classpath of the run to a file.");
    }

    @TaskAction
    public void run() throws Exception {
        final File out = ensureFileWorkspaceReady(getOutput());
        Files.write(
                out.toPath(),
                getInputFiles().getFiles().stream()
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toSet()),
                StandardCharsets.UTF_8
        );

        getLogger().lifecycle("Serialized classpath to: {}", out.getAbsolutePath());
    }

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInputFiles();
}
