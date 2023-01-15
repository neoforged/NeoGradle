package net.minecraftforge.gradle.common.runtime.tasks.action;

import net.minecraftforge.gradle.base.util.FileUtils;
import net.minecraftforge.gradle.base.util.GradleInternalUtils;
import net.minecraftforge.gradle.base.util.HashFunction;
import net.minecraftforge.gradle.base.util.UrlUtils;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.CopyProgressEvent;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
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
import java.net.URL;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ExtractFileAction implements WorkAction<ExtractFileAction.Params> {
    private static final Logger LOGGER = Logging.getLogger(ExtractFileAction.class);

    @Override
    public void execute() {
        try {
            final Params params = getParameters();
            final File output = params.getOutputDirectory().get().getAsFile();
            final GradleInternalUtils.ProgressLoggerWrapper progress = GradleInternalUtils.getProgressLogger(LOGGER, params.getBuildServiceRegistry(), "Extracting file: " + params.getInputFile().get().getAsFile());
            progress.setActionType("analyzed");
            progress.setDestFileName(output.getName());

            FileUtils.extractZip(
                    params.getArchiveOperations(),
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
        @SuppressWarnings("UnstableApiUsage")
        @Inject
        BuildServiceRegistry getBuildServiceRegistry();
        @Inject
        ArchiveOperations getArchiveOperations();
    }
}
