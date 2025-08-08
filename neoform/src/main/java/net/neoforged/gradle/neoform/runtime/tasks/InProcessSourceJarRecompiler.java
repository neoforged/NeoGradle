package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class InProcessSourceJarRecompiler extends DefaultRuntime
{

    @InputFile
    public abstract RegularFileProperty getSourceToCompile();

    @InputFiles
    @Optional
    @CompileClasspath
    public abstract ConfigurableFileCollection getAdditionalSources();

    @InputFiles
    @Optional
    @CompileClasspath
    public abstract ConfigurableFileCollection getClasspath();

    @Nested
    @Optional
    @Override
    public abstract Property<JavaLanguageVersion> getJavaVersion();

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void compile() {
        try {
            getCacheService().get()
                .cached(
                    this,
                    ICacheableJob.Default.file(
                        this::doCompile, getOutput()
                    )
                ).execute();
        } catch (IOException e) {
            throw new GradleException("Failed to recompile!", e);
        }
    }
    private void doCompile() throws Exception {
        final var sources = getSourceToCompile().get().getAsFile().toPath();

        final var additionalSources = getAdditionalSources().getFiles()
            .stream()
            .map(File::toPath)
            .toList();

        final var compileClasspath = getClasspath().getFiles()
            .stream()
            .map(File::toPath)
            .toList();

        URI uri = URI.create("jar:" + sources.toUri());
        try (var sourceFs = FileSystems.newFileSystem(uri, Map.of())) {
            var compiler = ToolProvider.getSystemJavaCompiler();

            long start = System.currentTimeMillis();

            var sourceRoot = sourceFs.getRootDirectories().iterator().next();
            List<Path> sourcePaths = new ArrayList<>();
            List<Path> nonSourcePaths = new ArrayList<>();
            try (var stream = Files.walk(sourceRoot).filter(Files::isRegularFile)) {
                stream.forEach(path -> {
                    var filename = path.getFileName().toString();
                    if (filename.endsWith(".java")) {
                        sourcePaths.add(path);
                    } else {
                        nonSourcePaths.add(path);
                    }
                });
            }

            getLogger().error(" Compiling {} source files", sourcePaths.size());

            var diagnostics = new DiagnosticListener<JavaFileObject>() {
                @Override
                public void report(Diagnostic<? extends JavaFileObject> d) {
                    if (d.getKind() != Diagnostic.Kind.ERROR) {
                        return; // Ignore anything but errors unless we're in verbose mode
                    }

                    var location = d.getSource() != null ? d.getSource().getName() : "<unknown>";
                    getLogger().error(" {} Line: {}, {} in {}", d.getKind(), d.getLineNumber(), d.getMessage(null), location);
                }
            };

            long prepare = System.currentTimeMillis();
            getLogger().error("Complete compile prepare in: {}ms.", prepare - start);

            var outputPath = getOutput().get().getAsFile().toPath();
            try (var outputFs = FileSystems.newFileSystem(URI.create("jar:" + outputPath.toUri()), Map.of("create", true))) {
                var outputRoot = outputFs.getRootDirectories().iterator().next();

                try (var fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
                    fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Collections.singleton(outputRoot));
                    fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, compileClasspath);
                    fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, additionalSources);

                    var sourceJavaFiles = fileManager.getJavaFileObjectsFromPaths(sourcePaths);
                    var task = compiler.getTask(null, fileManager, diagnostics, getCompilerOptions(), null, sourceJavaFiles);
                    if (!task.call()) {
                        throw new IOException("Compilation failed");
                    }
                }

                long compiled = System.currentTimeMillis();
                getLogger().error("Completed compile in: {}ms.", compiled - prepare);

                // Copy over all non-java files as well
                for (var nonSourcePath : nonSourcePaths) {
                    var relativeDestinationPath = sourceRoot.relativize(nonSourcePath).toString().replace('\\', '/');
                    var destination = outputRoot.resolve(relativeDestinationPath);
                    Files.createDirectories(destination.getParent());
                    Files.copy(nonSourcePath, destination);
                }
                getLogger().error("Copied {} resource files", nonSourcePaths.size());
                getLogger().error("Complete compile: {}ms.", System.currentTimeMillis() - compiled);
            }
        }
    }

    private List<String> getCompilerOptions() {
        return List.of(
            "--release",
            getJavaVersion().get().toString(),
            "-proc:none", // No annotation processing on Minecraft sources
            "-nowarn", // We have no influence on Minecraft sources, so no warnings
            "-g", // Gradle compiles with debug by default, so we replicate this
            "-XDuseUnsharedTable=true", // Gradle also adds this unconditionally
            "-implicit:none" // Prevents source files from the source-path from being emitted
        );
    }
}
