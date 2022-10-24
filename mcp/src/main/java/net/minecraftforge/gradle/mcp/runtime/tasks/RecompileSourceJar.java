package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.mcp.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.internal.CurrentJvmToolchainSpec;

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
        getOutputFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("outputExtension", getProject().provider(() -> "jar")).map(extension -> String.format("output.%s", extension))));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        getRuntimeJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
        getRuntimeJavaLauncher().convention(getJavaToolChain().flatMap(toolChain -> {
            if (!getRuntimeJavaVersion().isPresent()) {
                return toolChain.launcherFor(new CurrentJvmToolchainSpec(getProject().getObjects()));
            }

            return toolChain.launcherFor(spec -> spec.getLanguageVersion().set(getRuntimeJavaVersion()));
        }));
        getLogging().captureStandardOutput(LogLevel.DEBUG);
        getLogging().captureStandardError(LogLevel.ERROR);

        setDescription("Recompiles an already existing decompiled java jar.");
        setSource(getProject().zipTree(getInputJar()).matching(filter -> filter.include("**/*.java")));

        setClasspath(getCompileClasspath());
        getOptions().setAnnotationProcessorPath(getAnnotationProcessorPath());

        getOptions().getGeneratedSourceOutputDirectory().convention(getOutputDirectory().map(directory -> directory.dir("generated/sources/annotationProcessor")));
        getOptions().getHeaderOutputDirectory().convention(getOutputDirectory().map(directory -> directory.dir("generated/sources/headers")));

        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);

        getModularity().getInferModulePath().convention(javaPluginExtension.getModularity().getInferModulePath());
        getJavaCompiler().convention(getRuntimeJavaVersion().flatMap(javaVersion -> service.compilerFor(javaToolchainSpec -> javaToolchainSpec.getLanguageVersion().set(javaVersion))));

        getDestinationDirectory().set(getOutputDirectory().map(directory -> directory.dir("classes")));

        getOptions().setWarnings(false);
        getOptions().setVerbose(false);
        getOptions().setDeprecation(false);
        getOptions().setFork(true);

        //Leave this as an anon class, so that gradle is aware of this. Lambdas can not be used during task tree analysis.
        //noinspection Convert2Lambda
        doLast(new Action<Task>() {
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

    @Internal
    public final Provider<JavaToolchainService> getJavaToolChain() {
        return getProject().provider(() -> getProject().getExtensions().getByType(JavaToolchainService.class));
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
