package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.common.util.FileUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.minecraftforge.gradle.common.util.ZipBuildingFileTreeVisitor;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class Inject extends Runtime {
    private static final CacheableMinecraftVersion v1_14_4 = CacheableMinecraftVersion.from("1.14.4");

    public Inject() {
        super();

        getInjectionDirectory().fileProvider(getRuntimeData().map(data -> data.get("inject")));
    }

    @TaskAction
    public void run() throws Exception {
        final Provider<RegularFile> inputZipFile = getInjectionSource();
        final File outputFile = ensureFileWorkspaceReady(getOutput());

        final Directory injectionDirectory = getInjectionDirectory().get();
        final RegularFile packageInfoTemplate = injectionDirectory.file("package-info-template.java");
        final String packageInfoTemplateContent = packageInfoTemplate.getAsFile().exists() ? FileUtils.readAllLines(packageInfoTemplate.getAsFile().toPath()).collect(Collectors.joining("\n")) : null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputZipFile.get().getAsFile()));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {

            Set<String> visited = new HashSet<>();

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(entry);
                IOUtils.copyLarge(zis, zos);
                zos.closeEntry();

                if (packageInfoTemplateContent != null) {
                    String pkg = entry.isDirectory() && !entry.getName().endsWith("/") ? entry.getName() : entry.getName().indexOf('/') == -1 ? "" : entry.getName().substring(0, entry.getName().lastIndexOf('/'));
                    if (visited.add(pkg)) {
                        if (!pkg.startsWith("net/minecraft/") &&
                                (!pkg.startsWith("com/mojang/") || getMinecraftVersion().get().compareTo(v1_14_4) <= 0)) //Add com/mojang package-infos in 1.15+, could probably get away without the version check
                            continue;
                        zos.putNextEntry(Utils.getStableEntry(pkg + "/package-info.java"));
                        zos.write(packageInfoTemplateContent.replace("{PACKAGE}", pkg.replaceAll("/", ".")).getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();
                    }
                }
            }

            getFilteredInjectionDirectory().visit(new ZipBuildingFileTreeVisitor(zos));
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInjectionSource();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getInjectionDirectory();

    @Input
    @Optional
    public abstract Property<String> getInclusionFilter();

    private FileTree getFilteredInjectionDirectory() {
        if (!getInclusionFilter().isPresent())
            return getInjectionDirectory().getAsFileTree();

        return getInjectionDirectory().getAsFileTree().matching(pattern -> pattern.include(getInclusionFilter().get()));
    }
}
