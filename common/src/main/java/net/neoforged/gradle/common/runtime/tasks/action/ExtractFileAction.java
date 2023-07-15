package net.neoforged.gradle.common.runtime.tasks.action;

import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.GradleInternalUtils;
import org.gradle.api.Action;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import javax.inject.Inject;
import java.io.File;
import java.util.function.Function;

public abstract class ExtractFileAction implements WorkAction<ExtractFileAction.Params> {
    private static final Logger LOGGER = Logging.getLogger(ExtractFileAction.class);

    @Inject
    public abstract BuildServiceRegistry getBuildServiceRegistry();

    @Inject
    public abstract ArchiveOperations getArchiveOperations();

    @Override
    public void execute() {
        try {
            final Params params = getParameters();
            final File output = params.getOutputDirectory().get().getAsFile();
            final GradleInternalUtils.ProgressLoggerWrapper progress = GradleInternalUtils.getProgressLogger(LOGGER, getBuildServiceRegistry(), "Extracting file: " + params.getInputFile().get().getAsFile());
            progress.setActionType("analyzed");
            progress.setDestFileName(output.getName());

            FileUtils.extractZip(
                    getArchiveOperations(),
                    params.getInputFile().get().getAsFile(),
                    params.getOutputDirectory().get().getAsFile(),
                    params.getShouldOverride().get(),
                    params.getShouldCleanTarget().get(),
                    params.getFilter().get(),
                    params.getRenamer().get(),
                    progress
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface Params extends WorkParameters {
        RegularFileProperty getInputFile();
        Property<Boolean> getShouldOverride();
        Property<Boolean> getShouldCleanTarget();
        Property<Function<String, String>> getRenamer();
        Property<Action<? super PatternFilterable>> getFilter();
        DirectoryProperty getOutputDirectory();
    }
}
