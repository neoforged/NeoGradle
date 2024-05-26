package net.neoforged.gradle.userdev.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
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
                getInputFiles()
                        .getAsFileTree()
                        //Filter out valid classpath elements, this can put .pom files in the input files, so we need to remove those.
                        .matching(filter -> {
                            filter.include(fileTreeElement -> fileTreeElement.isDirectory() ||
                                    fileTreeElement.getName().endsWith(".jar") ||
                                    fileTreeElement.getName().endsWith(".zip"));
                        })
                        .getFiles().stream()
                        .map(File::getAbsolutePath)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                StandardCharsets.UTF_8
        );
    }

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInputFiles();
}
