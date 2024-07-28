package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.util.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Inject additional files into a Zip (or Jar) file.
 */
@CacheableTask
public abstract class InjectZipContent extends DefaultRuntime {

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void run() throws Throwable {
        getCacheService().get()
                .cached(
                        this,
                        ICacheableJob.Default.file(
                                getOutput(),
                                () -> {
                                    final Provider<RegularFile> inputZipFile = getInjectionSource();
                                    final File outputFile = ensureFileWorkspaceReady(getOutput());

                                    injectCode(inputZipFile.get().getAsFile(), outputFile);
                                }
                        )
                ).execute();
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInjectionSource();

    @Nested
    public abstract ListProperty<AbstractInjectSource> getInjectedSources();

    private void injectCode(File inputZipFile, File outputZipFile) throws IOException {

        List<AbstractInjectSource> injectedSources = getInjectedSources().get();

        String packageInfoTemplateContent = findPackageInfoTemplate(injectedSources);

        try (OutputStream fileOut = new FileOutputStream(outputZipFile);
             ZipOutputStream zos = new ZipOutputStream(fileOut)) {

            copyInputZipContent(inputZipFile, zos, packageInfoTemplateContent);

            // Copy over the injection sources
            for (AbstractInjectSource injectedSource : injectedSources) {
                injectedSource.copyTo(zos);
            }
        }
    }

    /*
     * We support automatically adding package-info.java files to the source jar based on a template-file
     * found in any one of the inject directories.
     */
    @Nullable
    private String findPackageInfoTemplate(List<AbstractInjectSource> injectedSources) throws IOException {
        // Try to find a package-info-template.java
        for (AbstractInjectSource injectedSource : injectedSources) {
            byte[] content = injectedSource.tryReadFile("package-info-template.java");
            if (content != null) {
                return new String(content, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /*
     * Copies the original ZIP content while applying the optional package-info.java transform.
     */
    private void copyInputZipContent(File inputZipFile, ZipOutputStream zos, @Nullable String packageInfoTemplateContent) throws IOException {
        Set<String> visited = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputZipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(entry);
                IOUtils.copyLarge(zis, zos);
                zos.closeEntry();

                if (packageInfoTemplateContent != null) {
                    String pkg = entry.isDirectory() && !entry.getName().endsWith("/") ? entry.getName() : entry.getName().indexOf('/') == -1 ? "" : entry.getName().substring(0, entry.getName().lastIndexOf('/'));
                    if (visited.add(pkg)) {
                        if (!pkg.startsWith("net/minecraft/") &&
                                !pkg.startsWith("com/mojang/")) {
                            continue;
                        }
                        zos.putNextEntry(FileUtils.getStableEntry(pkg + "/package-info.java"));
                        zos.write(packageInfoTemplateContent.replace("{PACKAGE}", pkg.replaceAll("/", ".")).getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    /**
     * Configures this task to inject the content of the given Zip-file matching the given filter.
     */
    public void injectZip(Provider<File> zipFile, Consumer<PatternFilterable> filter) {
        InjectFromZipSource zipInject = getProject().getObjects().newInstance(InjectFromZipSource.class);
        zipInject.getZipFile().fileProvider(zipFile);

        addSource(zipInject, filter);
    }

    /**
     * Configures this task to inject the content of the given directory.
     */
    public void injectFileTree(FileTree directory) {
        InjectFromFileTreeSource zipInject = getProject().getObjects().newInstance(InjectFromFileTreeSource.class);
        zipInject.getFiles().from(directory);

        addSource(zipInject, filter -> {
        });
    }

    private void addSource(AbstractInjectSource zipInject, Consumer<PatternFilterable> filter) {
        // Delay call to filter
        Provider<PatternFilterable> patternProvider = getProject().provider(() -> {
            PatternSet patternSet = new PatternSet();
            filter.accept(patternSet);
            return patternSet;
        });

        // Sort to avoid problems with up-to-date checks
        zipInject.getInclusionFilter().set(patternProvider.map(pf -> pf.getIncludes().stream().sorted().collect(Collectors.toList())));
        zipInject.getExclusionFilter().set(patternProvider.map(pf -> pf.getExcludes().stream().sorted().collect(Collectors.toList())));
        getInjectedSources().add(zipInject);
    }

}
