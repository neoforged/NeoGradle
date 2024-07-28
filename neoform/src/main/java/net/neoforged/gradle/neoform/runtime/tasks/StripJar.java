package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.util.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

@CacheableTask
public abstract class StripJar extends DefaultRuntime {

    public StripJar() {
        super();

        getMappingsFiles().from(getRuntimeData().map(data -> data.get("mappings")));
        getIsWhitelistMode().convention(true);
        getFilters().convention(
                getProject().provider(() -> {
                    if (getMappingsFiles().isEmpty()) {
                        return null;
                    }

                    return getMappingsFiles().getFiles()
                            .stream()
                            .flatMap(file -> FileUtils.readAllLines(file.toPath()))
                            .filter(l -> !l.startsWith("\t"))
                            .map(s -> s.split(" ")[0] + ".class")
                            .distinct()
                            .collect(Collectors.toList());
                })
        );

        getIsWhitelistMode().finalizeValueOnRead();
        getFilters().finalizeValueOnRead();
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    protected void run() throws Throwable {
        getCacheService().get().cached(
                this,
                ICacheableJob.Default.file(getOutput(), this::doRun)
        ).execute();
    }

    protected void doRun() throws Exception {
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutput());
        final boolean isWhitelist = getIsWhitelistMode().get();

        strip(input, output, isWhitelist);
    }

    private void strip(File input, File output, boolean whitelist) throws IOException {
        try (JarInputStream is = new JarInputStream(new FileInputStream(input));
             FileOutputStream fout = new FileOutputStream(output);
             JarOutputStream os = new JarOutputStream(fout)) {

            // Ignore any entry that's not allowed
            JarEntry entry;
            while ((entry = is.getNextJarEntry()) != null) {
                if (!isEntryValid(entry, whitelist)) {
                    continue;
                }
                os.putNextEntry(entry);
                IOUtils.copyLarge(is, os);
                os.closeEntry();
            }
        }
    }

    private boolean isEntryValid(JarEntry entry, boolean whitelist) {
        if (entry.isDirectory())
            return false;

        if (getFilters().isPresent()) {
            return getFilters().get().contains(entry.getName()) == whitelist;
        }

        return true;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    @Optional
    public abstract ConfigurableFileCollection getMappingsFiles();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    @Optional
    public abstract ListProperty<String> getFilters();

    @Input
    public abstract Property<Boolean> getIsWhitelistMode();
}
