package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.dsl.common.util.Constants;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.util.Collections;
import java.util.List;

@CacheableTask
public abstract class AccessTransformer extends Execute {

    public AccessTransformer() {
        super();

        setDescription("Runs the access transformer on the decompiled sources.");

        getData().set(Collections.emptyMap());
        getExecutingArtifact().convention(Constants.ACCESSTRANSFORMER);
        getRuntimeProgramArguments().convention(
                getInputFile().map(inputFile -> {
                            final List<String> args = Lists.newArrayList();
                            final File outputFile = ensureFileWorkspaceReady(getOutput());

                            args.add("--inJar");
                            args.add(inputFile.getAsFile().getAbsolutePath());
                            args.add("--outJar");
                            args.add(outputFile.getAbsolutePath());
                            getTransformers().forEach(f -> {
                                args.add("--atFile");
                                args.add(f.getAbsolutePath());
                            });

                            return args;
                        }
                )
        );

        getTransformers().finalizeValueOnRead();
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();


    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTransformers();
}
