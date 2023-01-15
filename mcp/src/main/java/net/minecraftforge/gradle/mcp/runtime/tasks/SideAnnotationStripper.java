package net.minecraftforge.gradle.mcp.runtime.tasks;

import com.google.common.collect.Lists;
import net.minecraftforge.gradle.base.util.TransformerUtils;
import net.minecraftforge.gradle.common.runtime.tasks.Execute;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

@CacheableTask
public abstract class SideAnnotationStripper extends Execute {

    public SideAnnotationStripper() {
        super();

        getExecutingArtifact().convention(Constants.SIDESTRIPPER);
        getRuntimeProgramArguments().convention(
                getInputFile().map(inputFile -> {
                    final List<String> args = Lists.newArrayList();
                    final File outputFile = ensureFileWorkspaceReady(getOutput());
                    args.add("--strip");
                    args.add("--input");
                    args.add(inputFile.getAsFile().getAbsolutePath());
                    args.add("--output");
                    args.add(outputFile.getAbsolutePath());
                    getDataFiles().forEach(f -> {
                        args.add("--data");
                        args.add(f.getAbsolutePath());
                    });

                    final Provider<File> additionalDataEntriesFileProvider = getAdditionalDataEntries()
                            .flatMap(additionalDataEntries -> transformEnsureFileWorkspaceReady(getFileInOutputDirectory(getProjectFileName("data.sas")))
                                    .map(TransformerUtils.guard(
                                            TransformerUtils.peakWithThrow(additionalDataEntriesFile ->
                                                    Files.write(additionalDataEntriesFile.toPath(), additionalDataEntries, StandardOpenOption.CREATE_NEW))
                                    )));

                    if (additionalDataEntriesFileProvider.isPresent()) {
                        args.add("--data");
                        args.add(additionalDataEntriesFileProvider.get().getAbsolutePath());
                    }


                    return args;
                }));
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getDataFiles();

    @Input
    @Optional
    public abstract ListProperty<String> getAdditionalDataEntries();
}
