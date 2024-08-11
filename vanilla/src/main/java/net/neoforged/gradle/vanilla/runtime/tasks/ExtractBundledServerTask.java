package net.neoforged.gradle.vanilla.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@CacheableTask
public abstract class ExtractBundledServerTask extends DefaultRuntime {

    @TaskAction
    public void run() throws Exception {
        final String minecraftVersion = getMinecraftVersion().get().toString();

        final File output = ensureFileWorkspaceReady(getOutput());

        try (final JarFile zipFile = new JarFile(getInput().getAsFile().get())) {
            final ZipEntry entry = zipFile.getEntry(String.format("META-INF/versions/%s/server-%s.jar", minecraftVersion, minecraftVersion));
            if (entry == null) {
                FileUtils.copyFile(getInput().getAsFile().get(), output);
                return;
            }

            final InputStream serverStream = zipFile.getInputStream(entry);
            FileUtils.copyInputStreamToFile(serverStream, output);
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<String> getMinecraftVersion();
}
