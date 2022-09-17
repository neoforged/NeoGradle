package net.minecraftforge.gradle.mcp.runtime.tasks;

import com.google.common.collect.Lists;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.common.util.Utils;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

@CacheableTask
public abstract class SideAnnotationStripperTask extends ExecutingMcpRuntimeTask {

    public SideAnnotationStripperTask() {
        super();

        getExecutingArtifact().convention(Utils.SIDESTRIPPER);
        getProgramArguments().convention(
                getInputFile().flatMap(inputFile -> getOutputFile().map(outputFile -> {
                    final List<String> args = Lists.newArrayList();
                    args.add("--strip");
                    args.add("--input");
                    args.add(inputFile.getAsFile().getAbsolutePath());
                    args.add("--output");
                    args.add(outputFile.getAsFile().getAbsolutePath());
                    getDataFiles().forEach(f -> {
                        args.add("--data");
                        args.add(f.getAbsolutePath());
                    });

                    final Provider<File> additionalDataEntriesFileProvider = getAdditionalDataEntries()
                            .flatMap(additionalDataEntries -> ensureFileWorkspaceReady(getFileInOutputDirectory(getProjectFileName("data.sas")))
                                    .map(TransformerUtils.guard(
                                            TransformerUtils.peakWithThrow(additionalDataEntriesFile ->
                                                    Files.write(additionalDataEntriesFile.toPath(), additionalDataEntries, StandardOpenOption.CREATE_NEW))
                                    )));

                    if (additionalDataEntriesFileProvider.isPresent()) {
                        args.add("--data");
                        args.add(additionalDataEntriesFileProvider.get().getAbsolutePath());
                    }


                    return args;
                })));

        getDataFiles().finalizeValueOnRead();
        getAdditionalDataEntries().finalizeValueOnRead();
    }

    @Input
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getDataFiles();

    @Input
    @Optional
    public abstract ListProperty<String> getAdditionalDataEntries();
}
