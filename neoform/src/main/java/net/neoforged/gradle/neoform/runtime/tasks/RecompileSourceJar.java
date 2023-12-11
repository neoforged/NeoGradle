package net.neoforged.gradle.neoform.runtime.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CacheKey;
import net.neoforged.gradle.common.caching.SharedCacheService;
import net.neoforged.gradle.common.runtime.tasks.RuntimeArgumentsImpl;
import net.neoforged.gradle.common.runtime.tasks.RuntimeDataImpl;
import net.neoforged.gradle.common.runtime.tasks.RuntimeMultiArgumentsImpl;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeData;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeMultiArguments;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.internal.CurrentJvmToolchainSpec;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class RecompileSourceJar extends JavaCompile implements Runtime {

    private Property<JavaLanguageVersion> javaVersion;
    private Provider<JavaToolchainService> javaToolchainService;
    private final RuntimeData data;
    private final RuntimeArguments arguments;
    private final RuntimeMultiArguments multiArguments;

    public RecompileSourceJar() {
        super();
        
        data = getObjectFactory().newInstance(RuntimeDataImpl.class, getProviderFactory());
        arguments = getObjectFactory().newInstance(RuntimeArgumentsImpl.class, getProviderFactory());
        multiArguments = getObjectFactory().newInstance(RuntimeMultiArgumentsImpl.class, getProviderFactory());
        
        this.javaVersion = getProject().getObjects().property(JavaLanguageVersion.class);
        
        final JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);
        this.javaToolchainService = getProviderFactory().provider(() -> service);
        
        getRuntimeName().orElse("unknown");
        getRuntimeDirectory().convention(getProject().getLayout().getBuildDirectory().dir("mcp"));
        getUnpackedMcpZipDirectory().convention(getRuntimeDirectory().dir("unpacked"));
        getStepsDirectory().convention(getRuntimeDirectory().dir("steps"));

        //And configure output default locations.
        getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
        getOutputFileName().convention(getArguments().getOrDefault("outputExtension", getProviderFactory().provider(() -> "jar")).map(extension -> String.format("output.%s", extension)));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        getJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
        getJavaLauncher().convention(getJavaToolChain().flatMap(toolChain -> {
            if (!getJavaVersion().isPresent()) {
                return toolChain.launcherFor(new CurrentJvmToolchainSpec(getObjectFactory()));
            }

            return toolChain.launcherFor(spec -> spec.getLanguageVersion().set(getJavaVersion()));
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
    protected void compile(InputChanges inputs) {
        SharedCacheService cacheService = getSharedCacheService().get();

        CacheKey cacheKey = cacheService.cacheKeyBuilder(getProject())
                .cacheDomain(getStepName().get())
                // NOTE: we specifically do *not* use the task inputs here,
                // since they include all source files, but those are already included
                // in the input jar hash.
                .inputFiles(Lists.newArrayList(
                        getInputJar().getAsFile().get()
                        // Any other input (libraries, client-extra) could cause the compilation to succeed or fail,
                        // but should not change the generated files.
                ))
                .build();

        Path outputFile = getOutput().get().getAsFile().toPath();
        boolean usedCache;
        try {
            usedCache = cacheService.cacheOutput(getProject(), cacheKey, outputFile, () -> {
                super.compile(inputs);

                if (getDidWork()) {
                    final File outputJar = ensureFileWorkspaceReady(getOutput());
                    try (OutputStream fileOutputStream = new FileOutputStream(outputJar);
                         ZipOutputStream outputZipStream = new ZipOutputStream(fileOutputStream)) {
                        //Add the compiled output.
                        getDestinationDirectory().getAsFileTree().visit(new ZipBuildingFileTreeVisitor(outputZipStream));
                        //Add the original resources.
                        getArchiveOperations().zipTree(getInputJar())
                                .matching(filter -> filter.exclude("**/*.java"))
                                .visit(new ZipBuildingFileTreeVisitor(outputZipStream));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create recompiled output jar", e);
                    }
                }

                FileUtils.assertNonEmptyZipFile(outputFile.toFile());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (usedCache) {
            setDidWork(false);
        }
    }

    @ServiceReference(CommonProjectPlugin.NEOFORM_CACHE_SERVICE)
    protected abstract Property<SharedCacheService> getSharedCacheService();
    
    @Override
    public RuntimeData getData() {
        return data;
    }
    
    @Override
    public RuntimeArguments getArguments() {
        return arguments;
    }
    
    @Override
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
