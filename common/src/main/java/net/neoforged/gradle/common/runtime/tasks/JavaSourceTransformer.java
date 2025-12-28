package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import net.neoforged.gradle.util.DirectoryTreeBuildingFileTreeVisitor;
import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.*;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class JavaSourceTransformer extends DefaultExecute {

    public JavaSourceTransformer() {
        super();

        setDescription("Runs the access transformer on the decompiled sources.");

        getStubs().convention(getOutputDirectory().map(dir -> dir.file("stubs.jar")));
        getParchmentConflictPrefix().convention("p_");

        getExecutingClasspath().from(ToolUtilities.resolveTool(getProject(), getProject().getExtensions().getByType(Subsystems.class).getTools().getJST().get()));

        getTransformed().convention(getOutputDirectory().map(output -> output.dir("transformed")));

        getRuntimeProgramArguments().convention(
                getInputFile().map(inputFile -> {
                            final List<String> args = Lists.newArrayList();

                            final File outputFile = getTransformed().get().getAsFile();
                            if (outputFile.isFile())
                                outputFile.delete();

                            if (!outputFile.exists())
                                outputFile.mkdirs();

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
                                getInterfaceInjections().forEach(f -> {
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
                            args.add("--out-format=folder");

                            args.add(inputFile.getAsFile().getAbsolutePath());
                            args.add(outputFile.getAbsolutePath());

                            return args;
                        }
                )
        );

        getTransformers().finalizeValueOnRead();
    }

    @Override
    public void execute() throws Throwable
    {
        getCacheService().get()
            .cached(
                this,
                ICacheableJob.Default.of(
                    this::doExecute,
                    ICacheableJob.OutputEntry.directory(getTransformed().get().getAsFile()),
                    ICacheableJob.OutputEntry.file(getStubs().get().getAsFile())
                )
            )
            .withStage(
                ICacheableJob.Staged.file("pack", this::pack, getOutput())
            )
            .execute();
    }

    @Override
    protected List<RegularFileProperty> getCacheableOutputs()
    {
        final var superCaches = super.getCacheableOutputs();
        superCaches.add(getStubs());
        return superCaches;
    }

    @Override
    public void doExecute() throws Exception {
        //We need a separate check here that skips the execute call if there are no transformers.
        if (getTransformers().isEmpty() &&
            getInterfaceInjections().isEmpty() &&
            getParchmentMappings().isEmpty()) {

            //Unpack the input zip into the outputs:
            final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(getTransformed().get().getAsFile().toPath());
            getArchiveOperations().zipTree(getInputFile())
                    .visit(visitor);

            validateStubs();

            return;
        }

        super.doExecute();

        validateStubs();
    }

    private void validateStubs() throws IOException
    {
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

    private void pack() throws IOException
    {
        final FileTree outputTree = getObjectFactory().fileTree().from(getTransformed().get());
        try(final FileOutputStream fos = new FileOutputStream(ensureFileWorkspaceReady(getOutput()));
            final ZipOutputStream zos = new ZipOutputStream(fos)) {
            final ZipBuildingFileTreeVisitor visitor = new ZipBuildingFileTreeVisitor(zos);
            outputTree.visit(visitor);
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

    @OutputDirectory
    public abstract DirectoryProperty getTransformed();

    @Override
    public Provider<FileTree> getOutputAsTree()
    {
        final ObjectFactory factory = getObjectFactory();
        return getTransformed().map(it -> factory.fileTree().from(it));
    }
}
