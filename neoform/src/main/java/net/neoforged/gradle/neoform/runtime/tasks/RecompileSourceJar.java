package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.RuntimeArgumentsImpl;
import net.neoforged.gradle.common.runtime.tasks.RuntimeMultiArgumentsImpl;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeMultiArguments;
import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.internal.CurrentJvmToolchainSpec;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class RecompileSourceJar extends JavaCompile implements Runtime {

    private Property<JavaLanguageVersion> javaVersion;
    private Provider<JavaToolchainService> javaToolchainService;
    private final RuntimeArguments arguments;
    private final RuntimeMultiArguments multiArguments;

    public RecompileSourceJar() {
        super();

        arguments = getObjectFactory().newInstance(RuntimeArgumentsImpl.class, getProviderFactory());
        multiArguments = getObjectFactory().newInstance(RuntimeMultiArgumentsImpl.class, getProviderFactory());
        
        this.javaVersion = getProject().getObjects().property(JavaLanguageVersion.class);
        
        final JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);
        this.javaToolchainService = getProviderFactory().provider(() -> service);

        getStepsDirectory().convention(getRuntimeDirectory().dir("steps"));

        //And configure output default locations.
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
        getOutputFileName().convention(getArguments().getOrDefault("outputExtension", getProviderFactory().provider(() -> "jar")).map(extension -> String.format("output.%s", extension)));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        getJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
        getJavaLauncher().convention(getJavaToolChain().flatMap(toolChain -> {
            if (!getJavaVersion().isPresent()) {
                return toolChain.launcherFor(javaToolchainSpec -> javaToolchainSpec.getLanguageVersion().set(JavaLanguageVersion.of(Objects.requireNonNull(Jvm.current().getJavaVersion()).getMajorVersion())));
            }

            return toolChain.launcherFor(spec -> spec.getLanguageVersion().set(getJavaVersion()));
        }));

        setDescription("Recompiles an already existing decompiled java jar.");
        setSource(getProject().zipTree(getInputJar()).matching(filter -> filter.include("**/*.java")));

        setClasspath(getCompileClasspath());
        getOptions().setAnnotationProcessorPath(getAnnotationProcessorPath());

        getOptions().getGeneratedSourceOutputDirectory().convention(getOutputDirectory().map(directory -> directory.dir("generated/sources/annotationProcessor")));
        getOptions().getHeaderOutputDirectory().convention(getOutputDirectory().map(directory -> directory.dir("generated/sources/headers")));

        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);

        getModularity().getInferModulePath().convention(javaPluginExtension.getModularity().getInferModulePath());
        getJavaCompiler().convention(getJavaVersion().flatMap(javaVersion -> service.compilerFor(javaToolchainSpec -> javaToolchainSpec.getLanguageVersion().set(javaVersion))));

        getDestinationDirectory().set(getOutputDirectory().map(directory -> directory.dir("classes")));

        getOptions().setWarnings(false);
        getOptions().setVerbose(false);
        getOptions().setDeprecation(false);
        getOptions().setFork(true);
        getOptions().setIncremental(true);
        getOptions().getIncrementalAfterFailure().set(true);
        getInputJar().finalizeValueOnRead();
    }

    @Override
    @TaskAction
    protected void compile(InputChanges inputs) {
        super.compile(inputs);

        if (getState().getDidWork()) {
            final File outputJar = ensureFileWorkspaceReady(getOutput());
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputJar);
                ZipOutputStream outputZipStream = new ZipOutputStream(fileOutputStream)) {
                ZipBuildingFileTreeVisitor zipBuildingFileTreeVisitor = new ZipBuildingFileTreeVisitor(outputZipStream);
                //Add the compiled output.
                getDestinationDirectory().getAsFileTree().visit(zipBuildingFileTreeVisitor);
                //Add the original resources.
                getArchiveOperations().zipTree(getInputJar()).matching(filter -> filter.exclude("**/*.java")).visit(zipBuildingFileTreeVisitor);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create recompiled output jar", e);
            }
        }
    }

    
    @Override
    @Nested
    public RuntimeArguments getArguments() {
        return arguments;
    }
    
    @Override
    @Nested
    public RuntimeMultiArguments getMultiArguments() {
        return multiArguments;
    }
    
    @Override
    public String getGroup() {
        final String name = getRuntimeName().getOrElse("unknown");
        return String.format("NeoGradle/Runtime/%s", name);
    }

    @Internal
    public final Provider<JavaToolchainService> getJavaToolChain() {
        return javaToolchainService;
    }

    @Nested
    @Optional
    @Override
    public Property<JavaLanguageVersion> getJavaVersion() {
        return this.javaVersion;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputJar();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getAnnotationProcessorPath();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getCompileClasspath();
    
    @Override
    public abstract ObjectFactory getObjectFactory();
    
    @Override
    public abstract ProviderFactory getProviderFactory();
}
