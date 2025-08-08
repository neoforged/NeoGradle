package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class JavaSourceTransformer extends DefaultExecute {

    public JavaSourceTransformer() {
        super();

        setDescription("Runs the access transformer on the decompiled sources.");

        getStubs().convention(getOutputDirectory().map(dir -> dir.file("stubs.jar")));
        getParchmentConflictPrefix().convention("p_");

        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), getProject().getExtensions().getByType(Subsystems.class).getTools().getJST().get()));
        getRuntimeProgramArguments().convention(
                getInputFile().map(inputFile -> {
                            final List<String> args = Lists.newArrayList();
                            final File outputFile = ensureFileWorkspaceReady(getOutput());

                            if (!getTransformers().isEmpty()) {
                                args.add("--enable-accesstransformers");
                                getTransformers().forEach(f -> {
                                    args.add("--access-transformer");
                                    args.add(f.getAbsolutePath());
                                });
                            }

                            if (!getInterfaceInjections().isEmpty()) {
                                final File stubsFile = ensureFileWorkspaceReady(getStubs());

                                args.add("--enable-interface-injection");
                                getTransformers().forEach(f -> {
                                    args.add("--interface-injection-data");
                                    args.add(f.getAbsolutePath());
                                });

                                args.add("--interface-injection-stubs");
                                args.add(stubsFile.getAbsolutePath());
                            }

                            if (!getParchmentMappings().isEmpty()) {
                                final File parchment = getParchmentMappings().getSingleFile();
                                final String conflictPrefix = getParchmentConflictPrefix().getOrElse("p_");

                                args.add("--enable-parchment");
                                args.add("--parchment-mappings");
                                args.add(parchment.getAbsolutePath());
                                args.add("--parchment-conflict-prefix=%s".formatted(conflictPrefix));
                            }

                            args.add("--libraries-list=" + getLibraries().get().getAsFile().getAbsolutePath());

                            final StringBuilder builder = new StringBuilder();
                            getClasspath().forEach(f -> {
                                if (!builder.isEmpty()) {
                                    builder.append(File.pathSeparator);
                                }
                                builder.append(f.getAbsolutePath());
                            });
                            args.add("--classpath=" + builder);

                            args.add("--in-format=archive");
                            args.add("--out-format=archive");

                            args.add(inputFile.getAsFile().getAbsolutePath());
                            args.add(outputFile.getAbsolutePath());

                            return args;
                        }
                )
        );

        getTransformers().finalizeValueOnRead();
        getLogLevel().set(LogLevel.DISABLED);
    }

    @Override
    public void doExecute() throws Exception {
        //We need a separate check here that skips the execute call if there are no transformers.
        if (getTransformers().isEmpty() &&
            getInterfaceInjections().isEmpty() &&
            getParchmentMappings().isEmpty()) {
            final File output = ensureFileWorkspaceReady(getOutput());
            FileUtils.copyFile(getInputFile().get().getAsFile(), output);
        }

        super.doExecute();

        //Double check if the stubs file exists.
        final File stubs = getStubs().getAsFile().get();
        if (!stubs.exists()) {
            //noinspection EmptyTryBlock
            try (var stubsOutputStream = new FileOutputStream(stubs);
                 var ignored = new ZipOutputStream(stubsOutputStream)
            )
            {
            }
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputFile();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLibraries();

    @InputFiles
    @Optional
    @CompileClasspath
    public abstract ConfigurableFileCollection getClasspath();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getTransformers();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInterfaceInjections();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getParchmentMappings();

    @Optional
    @Input
    public abstract Property<String> getParchmentConflictPrefix();

    @OutputFile
    public abstract RegularFileProperty getStubs();
}
