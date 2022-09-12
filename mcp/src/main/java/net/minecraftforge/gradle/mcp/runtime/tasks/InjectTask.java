package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.util.FileUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.util.CacheableMinecraftVersion;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class InjectTask extends McpRuntimeTask {
    private static final CacheableMinecraftVersion v1_14_4 = CacheableMinecraftVersion.from("1.14.4");


    public InjectTask() {
        super();

        getInjectionDirectoryPrefix().convention(getRuntimeData().map(data -> data.get("inject").toString()));
        getInjectionFiles().convention(
                getUnpackedMcpZipDirectory().map(directory -> directory.getAsFileTree().matching(pattern -> {
                    pattern.include(getInjectionDirectoryPrefix().get() + "**");
                }).getFiles().stream().collect(Collectors.toMap(
                        file -> file.getAbsolutePath().replace(directory.getAsFile().getAbsolutePath(), "").replace(getInjectionDirectoryPrefix().get(), ""),
                        file -> FileUtils.readAllBytes(file.toPath()))))
        );
        getPackageInfoTemplate().convention(
                getInjectionFiles().map(injectionFiles -> {
                    if (injectionFiles.containsKey("package-info-template.java")) {
                        final String template = new String(injectionFiles.get("package-info-template.java"));
                        injectionFiles.remove("package-info-template.java");
                        return template;
                    } else {
                        //noinspection ConstantConditions - This is a default value, so it will never be null
                        return null;
                    }
                })
        );

        getInjectionDirectoryPrefix().finalizeValueOnRead();
        getInjectionFiles().finalizeValueOnRead();
        getPackageInfoTemplate().finalizeValueOnRead();
        getInjectionSource().finalizeValueOnRead();
    }

    @TaskAction
    public void run() throws Exception {
        Provider<RegularFile> inputZipFile = getInjectionSource();
        Provider<File> outputFile = ensureFileWorkspaceReady(getOutputFile());

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputZipFile.get().getAsFile()));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile.get()))) {

            Set<String> visited = new HashSet<>();

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(entry);
                IOUtils.copyLarge(zis, zos);
                zos.closeEntry();
                if (getPackageInfoTemplate().isPresent()) {
                    String pkg = entry.isDirectory() && !entry.getName().endsWith("/") ? entry.getName() : entry.getName().indexOf('/') == -1 ? "" : entry.getName().substring(0, entry.getName().lastIndexOf('/'));
                    if (visited.add(pkg)) {
                        if (!pkg.startsWith("net/minecraft/") &&
                                (!pkg.startsWith("com/mojang/") || getMinecraftVersion().get().compareTo(v1_14_4) <= 0)) //Add com/mojang package-infos in 1.15+, could probably get away without the version check
                            continue;
                        zos.putNextEntry(Utils.getStableEntry(pkg + "/package-info.java"));
                        zos.write(getPackageInfoTemplate().get().replace("{PACKAGE}", pkg.replaceAll("/", ".")).getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();
                    }
                }
            }

            for (Map.Entry<String, byte[]> add : getInjectionFiles().get().entrySet()) {
                boolean filter = "server".equals(getSide().get()) ? add.getKey().contains("/client/") : add.getKey().contains("/server/");
                if (filter)
                    continue;
                ZipEntry info = new ZipEntry(add.getKey());
                info.setTime(0);
                zos.putNextEntry(info);
                zos.write(add.getValue());
                zos.closeEntry();
            }
        }
    }

    @Inject
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInjectionSource();

    @Inject
    public abstract Property<String> getInjectionDirectoryPrefix();

    @Inject
    public abstract Property<String> getPackageInfoTemplate();

    @Input
    public abstract MapProperty<String, byte[]> getInjectionFiles();
}
