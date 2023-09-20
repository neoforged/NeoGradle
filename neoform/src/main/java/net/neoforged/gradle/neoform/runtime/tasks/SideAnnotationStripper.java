package net.neoforged.gradle.neoform.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.dsl.common.util.Constants;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.internal.impldep.org.glassfish.jaxb.runtime.v2.runtime.reflect.opt.Const;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

@CacheableTask
public abstract class SideAnnotationStripper extends Execute {

    public SideAnnotationStripper() {
        super();

        getAdditionalDataEntriesFile().set(getOutputDirectory().map(dir -> dir.file(getProjectFileName("data.sas"))));
        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), Constants.SIDESTRIPPER));
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
                            .flatMap(additionalDataEntries -> transformEnsureFileWorkspaceReady(getAdditionalDataEntriesFile().get().getAsFile())
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
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputFile();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getDataFiles();

    @Input
    @Optional
    public abstract ListProperty<String> getAdditionalDataEntries();
    
    @OutputFile
    public abstract RegularFileProperty getAdditionalDataEntriesFile();
    
}
