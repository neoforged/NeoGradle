package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

@CacheableTask
public abstract class AccessTransformerFileGenerator extends DefaultRuntime {

    public AccessTransformerFileGenerator() {
        super();

        getAdditionalTransformers().convention(
                getProject().getExtensions().getByType(AccessTransformers.class)
                        .getEntries()
        );

        getOutputFileName().set(String.format("_script_%s.cfg", getProject().getName()));
    }

    @TaskAction
    void doCreateAccessTransformerFiles() throws IOException {
        final File outputFile = ensureFileWorkspaceReady(getOutput());
        Files.deleteIfExists(outputFile.toPath());
        Files.write(outputFile.toPath(), getAdditionalTransformers().get(), StandardOpenOption.CREATE_NEW);

        if (!Files.exists(outputFile.toPath())) {
            Files.createFile(outputFile.toPath());
        }
    }

    @Input
    @Optional
    public abstract ListProperty<String> getAdditionalTransformers();

}
