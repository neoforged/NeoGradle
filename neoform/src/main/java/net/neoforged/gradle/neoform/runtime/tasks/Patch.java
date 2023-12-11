package net.neoforged.gradle.neoform.runtime.tasks;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CacheKey;
import net.neoforged.gradle.common.caching.SharedCacheService;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

@CacheableTask
public abstract class Patch extends DefaultRuntime {

    private static final Logger LOG = LoggerFactory.getLogger(Patch.class);

    public Patch() {
        super();

        getPatchDirectory().fileProvider(getRuntimeData().flatMap(data -> data.get("patches")));
        getRejectsFile().fileProvider(getFileInOutputDirectory("rejects.zip"));
        getIsVerbose().convention(false);
    }

    @TaskAction
    public void run() throws Exception {
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutput());
        final File rejects = getRejectsFile().get().getAsFile();

        SharedCacheService cachingService = getSharedCacheService().get();
        SharedCacheService.CacheKeyBuilder cacheKeyBuilder = cachingService.cacheKeyBuilder(getProject())
                // We use the NeoForm step name as the cache domain to make debugging the cache state easier
                // since every step will have its own cache directory.
                .cacheDomain(getStepName().get())
                .inputFiles(getInputs().getFiles().getFiles());
        Path toolJarPath = findJarForClass(PatchOperation.class);
        if (toolJarPath != null) {
            cacheKeyBuilder.tool(toolJarPath);
        }

        CacheKey cacheKey = cacheKeyBuilder.build();

        boolean usedCache = cachingService.cacheOutput(getProject(), cacheKey, output.toPath(), () -> {
            applyPatches(input, output, rejects);
        });

        if (usedCache) {
            setDidWork(false);
        }
    }

    private void applyPatches(File input, File output, File rejects) throws IOException {
        PatchOperation.Builder builder = PatchOperation.builder()
                .logTo(new LoggingOutputStream(getLogger(), LogLevel.LIFECYCLE))
                .basePath(input.toPath())
                .patchesPath(getUnpackedMcpZipDirectory().get().getAsFile().toPath())
                .patchesPrefix(getUnpackedMcpZipDirectory().get().getAsFile().toPath().relativize(getPatchDirectory().get().getAsFile().toPath()).toString())
                .outputPath(output.toPath())
                .level(getIsVerbose().get() ? codechicken.diffpatch.util.LogLevel.ALL : codechicken.diffpatch.util.LogLevel.WARN)
                .mode(PatchMode.OFFSET)
                .rejectsPath(rejects.toPath());

        if (getPatchesModifiedPrefix().isPresent()) {
            builder = builder.bPrefix(getPatchesModifiedPrefix().get());
        }

        if (getPatchesOriginalPrefix().isPresent()) {
            builder = builder.aPrefix(getPatchesOriginalPrefix().get());
        }

        CliOperation.Result<PatchOperation.PatchesSummary> result = builder.build().operate();

        boolean success = result.exit == 0;
        if (!success) {
            getProject().getLogger().error("Rejects saved to: {}", rejects);
            throw new RuntimeException("Patch failure.");
        }
    }

    @Nullable
    private Path findJarForClass(Class<?> classFile) {
        try {
            CodeSource src = classFile.getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();
                return Paths.get(jar.toURI());
            }
        } catch (URISyntaxException ignored) {
        }

        LOG.warn("Failed to determine the JAR-file containing tool class {}. This may cause cached files to become stale when the tool changes.",
                classFile);
        return null;
    }

    @ServiceReference(CommonProjectPlugin.NEOFORM_CACHE_SERVICE)
    protected abstract Property<SharedCacheService> getSharedCacheService();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    public abstract DirectoryProperty getPatchDirectory();

    @OutputFile
    public abstract RegularFileProperty getRejectsFile();

    @Input
    public abstract Property<Boolean> getIsVerbose();

    @Input
    @Optional
    public abstract Property<String> getPatchesOriginalPrefix();

    @Input
    @Optional
    public abstract Property<String> getPatchesModifiedPrefix();
}
