package net.neoforged.gradle.neoform.runtime.tasks;

import codechicken.diffpatch.cli.PatchOperation;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CacheKey;
import net.neoforged.gradle.common.caching.SharedCacheService;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

@CacheableTask
public abstract class StripJar extends DefaultRuntime {

    private static final String CACHE_GENERATION = "1";

    public StripJar() {
        super();

        getMappingsFile().fileProvider(getRuntimeData().flatMap(data -> data.get("mappings")));
        getIsWhitelistMode().convention(true);
        getFilters().convention(
                getMappingsFile().map(
                        TransformerUtils.guardWithResource(
                                lines -> lines.filter(l -> !l.startsWith("\t")).map(s -> s.split(" ")[0] + ".class").collect(Collectors.toSet()),
                                file -> FileUtils.readAllLines(file.getAsFile().toPath())
                        )
                )
        );

        getMappingsFile().finalizeValueOnRead();
        getIsWhitelistMode().finalizeValueOnRead();
        getFilters().finalizeValueOnRead();
    }

    @TaskAction
    protected void run() throws Throwable {

        SharedCacheService cachingService = getSharedCacheService().get();
        CacheKey cacheKey = cachingService.cacheKeyBuilder(getProject())
                // We use the NeoForm step name as the cache domain to make debugging the cache state easier
                // since every step will have its own cache directory.
                .cacheDomain(getStepName().get())
                .inputFiles(getInputs().getFiles().getFiles())
                .argument(CACHE_GENERATION)
                .build();

        final File output = ensureFileWorkspaceReady(getOutput());

        boolean usedCache = cachingService.cacheOutput(getProject(), cacheKey, output.toPath(), () -> {
            final File input = getInput().get().getAsFile();
            final boolean isWhitelist = getIsWhitelistMode().get();

            strip(input, output, isWhitelist);
        });

        if (usedCache) {
            setDidWork(false);
        }
    }

    @ServiceReference(CommonProjectPlugin.NEOFORM_CACHE_SERVICE)
    protected abstract Property<SharedCacheService> getSharedCacheService();

    private void strip(File input, File output, boolean whitelist) throws IOException {
        JarInputStream is = new JarInputStream(new FileInputStream(input));
        JarOutputStream os = new JarOutputStream(new FileOutputStream(output));

        // Ignore any entry that's not allowed
        JarEntry entry;
        while ((entry = is.getNextJarEntry()) != null) {
            if (!isEntryValid(entry, whitelist)) continue;
            os.putNextEntry(entry);
            IOUtils.copyLarge(is, os);
            os.closeEntry();
        }

        os.close();
        is.close();
    }

    private boolean isEntryValid(JarEntry entry, boolean whitelist) {
        return !entry.isDirectory() && getFilters().get().contains(entry.getName()) == whitelist;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getMappingsFile();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract ListProperty<String> getFilters();

    @Input
    public abstract Property<Boolean> getIsWhitelistMode();
}
