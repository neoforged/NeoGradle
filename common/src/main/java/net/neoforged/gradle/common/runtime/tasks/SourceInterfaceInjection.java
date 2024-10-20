package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.List;

@CacheableTask
public abstract class SourceInterfaceInjection extends DefaultExecute {

    public SourceInterfaceInjection()  {
        super();

        setDescription("Runs the interface injection on the decompiled sources.");

        getStubs().convention(getOutputDirectory().map(dir -> dir.file("stubs.jar")));

        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), getProject().getExtensions().getByType(Subsystems.class).getTools().getJST().get()));
        getRuntimeProgramArguments().convention(
                getInputFile().map(inputFile -> {
                            final List<String> args = Lists.newArrayList();
                            final File outputFile = ensureFileWorkspaceReady(getOutput());
                            final File stubsFile = ensureFileWorkspaceReady(getStubs());

                            args.add("--enable-interface-injection");
                            getTransformers().forEach(f -> {
                                args.add("--interface-injection-data");
                                args.add(f.getAbsolutePath());
                            });

                            args.add("--interface-injection-stubs");
                            args.add(stubsFile.getAbsolutePath());

                            args.add("--libraries-list=" + getLibraries().get().getAsFile().getAbsolutePath());

                            final StringBuilder builder = new StringBuilder();
                            getClasspath().forEach(f -> {
                                if (!builder.isEmpty()) {
                                    builder.append(File.pathSeparator);
                                }
                                builder.append(f.getAbsolutePath());
                            });
                            args.add("--classpath=" + builder);

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

    @Override
    public void doExecute() throws Exception {
        //We need a separate check here that skips the execute call if there are no transformers.
        if (getTransformers().isEmpty()) {
            final File output = ensureFileWorkspaceReady(getOutput());
            FileUtils.copyFile(getInputFile().get().getAsFile(), output);
        }

        super.doExecute();
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
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getTransformers();

    @OutputFile
    public abstract RegularFileProperty getStubs();
}
