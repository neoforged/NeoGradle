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
import java.util.Collections;
import java.util.List;

@CacheableTask
public abstract class AccessTransformer extends Execute {

    public AccessTransformer() {
        super();

        setDescription("Runs the access transformer on the decompiled sources.");

        getData().set(Collections.emptyMap());
        getExecutingArtifact().convention(Utils.ACCESSTRANSFORMER);
        getProgramArguments().convention(
                getInputFile().flatMap(inputFile ->
                        getOutput().map(outputFile -> {
                                    final List<String> args = Lists.newArrayList();
                                    args.add("--inJar");
                                    args.add(inputFile.getAsFile().getAbsolutePath());
                                    args.add("--outJar");
                                    args.add(outputFile.getAsFile().getAbsolutePath());
                                    getTransformers().forEach(f -> {
                                        args.add("--atFile");
                                        args.add(f.getAbsolutePath());
                                    });

                                    final Provider<File> additionalTransformersFileProvider = getAdditionalTransformers()
                                            .flatMap(additionalTransformers -> transformEnsureFileWorkspaceReady(getFileInOutputDirectory(getProjectFileName("transformers.cfg")))
                                                    .map(TransformerUtils.guard(
                                                            TransformerUtils.peakWithThrow(additionalTransformersFile ->
                                                                    Files.write(additionalTransformersFile.toPath(), additionalTransformers, StandardOpenOption.CREATE_NEW))
                                                    )));

                                    if (additionalTransformersFileProvider.isPresent()) {
                                        args.add("--atFile");
                                        args.add(additionalTransformersFileProvider.get().getAbsolutePath());
                                    }

                                    return args;
                                }

                        )
                ));

        getAdditionalTransformers().finalizeValueOnRead();
        getTransformers().finalizeValueOnRead();
    }

    @Input
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();

    @Input
    @Optional
    public abstract ListProperty<String> getAdditionalTransformers();

    @InputFiles
    public abstract ConfigurableFileCollection getTransformers();
}
