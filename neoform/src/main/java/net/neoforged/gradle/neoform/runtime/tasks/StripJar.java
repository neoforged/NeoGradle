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
import org.gradle.api.provider.SetProperty;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.*;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@CacheableTask
public abstract class StripJar extends DefaultRuntime {

    public StripJar() {
        super();

        getIsWhitelistMode().convention(true);
        getIsWhitelistMode().finalizeValueOnRead();
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
        try(var jar = new ZipFile(input);
            var outputStream = new JarOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(output)
                )
            )
        ) {
            var entries = jar.entries();
            while(entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory())
                    continue;

                if (entry.getName().endsWith(".class") != whitelist)
                    continue;

                outputStream.putNextEntry(entry);
                try (var is = jar.getInputStream(entry)) {
                    is.transferTo(outputStream);
                }
                outputStream.closeEntry();
            }
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<Boolean> getIsWhitelistMode();
}
