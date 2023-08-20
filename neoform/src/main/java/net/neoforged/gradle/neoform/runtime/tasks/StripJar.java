package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

@CacheableTask
public abstract class StripJar extends DefaultRuntime {

    public StripJar() {
        super();

        getMappingsFile().fileProvider(getRuntimeData().map(data -> data.get("mappings")));
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
        final File input = getInput().get().getAsFile();
        final File output = ensureFileWorkspaceReady(getOutput());
        final boolean isWhitelist = getIsWhitelistMode().get();

        strip(input, output, isWhitelist);
    }

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
