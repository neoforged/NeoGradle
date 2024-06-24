package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.List;

@CacheableTask
public abstract class SourceAccessTransformer extends DefaultExecute {

    public SourceAccessTransformer() {
        super();

        setDescription("Runs the access transformer on the decompiled sources.");

        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), getProject().getExtensions().getByType(Subsystems.class).getTools().getJST().get()));
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

                            final StringBuilder builder = new StringBuilder();
                            getClasspath().forEach(f -> {
                                if (!builder.isEmpty()) {
                                    builder.append(File.pathSeparator);
                                }
                                builder.append(f.getAbsolutePath());
                            });
                            args.add("--classpath=" + builder.toString());

                            args.add(inputFile.getAsFile().getAbsolutePath());
                            args.add(outputFile.getAbsolutePath());

                            return args;
                        }
                )
        );

        getJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
        getTransformers().finalizeValueOnRead();
        getLogLevel().set(LogLevel.DISABLED);
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputFile();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLibraries();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getClasspath();

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getTransformers();
}
