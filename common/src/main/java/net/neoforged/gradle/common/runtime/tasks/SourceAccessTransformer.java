package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.util.Constants;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.File;
import java.util.List;

@CacheableTask
public abstract class SourceAccessTransformer extends Execute {

    public SourceAccessTransformer() {
        super();

        setDescription("Runs the access transformer on the decompiled sources.");

        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), Constants.JST_TOOL_ARTIFACT));
        getRuntimeProgramArguments().convention(
                getInputFile().map(inputFile -> {
                            final List<String> args = Lists.newArrayList();
                            final File outputFile = ensureFileWorkspaceReady(getOutput());

                            args.add("--enable-accesstransformers");
                            getTransformers().forEach(f -> {
                                args.add("--access-transformer");
                                args.add(f.getAbsolutePath());
                            });

                            args.add("--libraries-list=" + getLibraries().get().getAsFile().getAbsolutePath());

                            args.add(inputFile.getAsFile().getAbsolutePath());
                            args.add(outputFile.getAbsolutePath());

                            return args;
                        }
                )
        );

        getJavaVersion().set(JavaLanguageVersion.of(17));

        getTransformers().finalizeValueOnRead();
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputFile();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLibraries();

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getTransformers();
}
