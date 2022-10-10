package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.mcp.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaToolchainService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class RecompileSourceJar extends JavaCompile implements IMcpRuntimeTask {

    public RecompileSourceJar() {
        super();

        getMcpDirectory().convention(getProject().getLayout().getBuildDirectory().dir("mcp"));
        getUnpackedMcpZipDirectory().convention(getMcpDirectory().dir("unpacked"));
        getStepsDirectory().convention(getMcpDirectory().dir("steps"));

        //And configure output default locations.
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
        getOutputFileName().convention(getArguments().map(arguments -> "output.%s".formatted(arguments.getOrDefault("outputExtension", "jar"))));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        setDescription("Recompiles an already existing decompiled java jar.");
        setSource(getProject().zipTree(getInputJar()).matching(filter -> filter.include("**/*.java")));

        setClasspath(getCompileClasspath());
        getOptions().setAnnotationProcessorPath(getAnnotationProcessorPath());

        getOptions().getGeneratedSourceOutputDirectory().convention(getOutputDirectory().map(directory -> directory.dir("generated/sources/annotationProcessor")));
        getOptions().getHeaderOutputDirectory().convention(getOutputDirectory().map(directory -> directory.dir("generated/sources/headers")));

        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);

        getModularity().getInferModulePath().convention(javaPluginExtension.getModularity().getInferModulePath());
        getJavaCompiler().convention(getJavaRuntimeVersion().flatMap(javaVersion -> service.compilerFor(javaToolchainSpec -> javaToolchainSpec.getLanguageVersion().set(javaVersion))));

        getDestinationDirectory().set(getOutputDirectory().map(directory -> directory.dir("classes")));

        getOptions().setWarnings(false);
        getOptions().setVerbose(false);
        getOptions().setDeprecation(false);
        getOptions().setFork(true);

        //Leave this as an anon class, so that gradle is aware of this. Lambdas can not be used during task tree analysis.
        //noinspection Convert2Lambda
        doLast(new Action<>() {
            @Override
            public void execute(Task doLast) {
                try {
                    final File outputJar = RecompileSourceJar.this.ensureFileWorkspaceReady(RecompileSourceJar.this.getOutput());
                    final FileOutputStream fileOutputStream = new FileOutputStream(outputJar);
                    final ZipOutputStream outputZipStream = new ZipOutputStream(fileOutputStream);
                    final ZipBuildingFileTreeVisitor zipBuildingFileTreeVisitor = new ZipBuildingFileTreeVisitor(outputZipStream);
                    //Add the compiled output.
                    RecompileSourceJar.this.getDestinationDirectory().getAsFileTree().visit(zipBuildingFileTreeVisitor);
                    //Add the original resources.
                    RecompileSourceJar.this.getProject().zipTree(RecompileSourceJar.this.getInputJar()).matching(filter -> filter.exclude("**/*.java")).visit(zipBuildingFileTreeVisitor);
                    outputZipStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create recompiled output jar", e);
                }
            }
        });
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputJar();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getAnnotationProcessorPath();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getCompileClasspath();
}
