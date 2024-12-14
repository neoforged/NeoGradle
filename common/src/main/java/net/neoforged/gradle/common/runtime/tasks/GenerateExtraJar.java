package net.neoforged.gradle.common.runtime.tasks;

import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.util.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class GenerateExtraJar extends NeoGradleBase implements WithOutput, WithWorkspace {

    public GenerateExtraJar() {
        super();
        getOutputFileName().set("client-extra.jar");
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void run() throws Throwable {
        getCacheService().get()
                .cached(
                        this,
                        ICacheableJob.Default.file(getOutput(), this::doRun)
                ).execute();
    }

    private void doRun() throws Exception {
        final File outputJar = ensureFileWorkspaceReady(getOutput());

        // Official mappings are Named -> Obf and need to be reversed
        var mappings = IMappingFile.load(getMappings().getAsFile().get()).reverse();
        try (var clientZip = new JarFile(getOriginalJar().getAsFile().get());
             var serverZip = new JarFile(getServerJar().getAsFile().get())) {
            var clientFiles = getFileIndex(clientZip);
            clientFiles.remove(JarFile.MANIFEST_NAME);
            var serverFiles = getFileIndex(serverZip);
            serverFiles.remove(JarFile.MANIFEST_NAME);

            var manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Minecraft-Dists", "server client");

            addSourceDistEntries(clientFiles, serverFiles, "client", mappings, manifest);
            addSourceDistEntries(serverFiles, clientFiles, "server", mappings, manifest);

            try (var zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputJar)))) {
                zos.putNextEntry(FileUtils.getStableEntry(JarFile.MANIFEST_NAME));
                manifest.write(zos);
                zos.closeEntry();

                // Generally ignore directories, manifests and class files
                var clientEntries = clientZip.entries();
                while (clientEntries.hasMoreElements()) {
                    var clientEntry = clientEntries.nextElement();
                    if (isResourceEntry(clientEntry)) {
                        zos.putNextEntry(clientEntry);
                        try (var clientIn = clientZip.getInputStream(clientEntry)) {
                            clientIn.transferTo(zos);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private static void addSourceDistEntries(Set<String> distFiles,
                                             Set<String> otherDistFiles,
                                             String dist,
                                             IMappingFile mappings,
                                             Manifest manifest) {
        for (var file : distFiles) {
            if (!otherDistFiles.contains(file)) {
                var fileAttr = new Attributes(1);
                fileAttr.putValue("Minecraft-Dist", dist);

                if (mappings != null && file.endsWith(".class")) {
                    file = mappings.remapClass(file.substring(0, file.length() - ".class".length())) + ".class";
                }
                manifest.getEntries().put(file, fileAttr);
            }
        }
    }

    private Set<String> getFileIndex(ZipFile zipFile) throws IOException {
        // Support "nested" ZIP-Files as in recent server jars
        var embeddedVersionPath = readEmbeddedVersionPath(zipFile);
        if (embeddedVersionPath != null) {
            // Extract the embedded zip and instead index that
            var versionJarEntry = zipFile.getEntry(embeddedVersionPath);
            if (versionJarEntry == null) {
                throw new IOException("version list in jar file " + zipFile + " refers to missing entry " + embeddedVersionPath);
            }
            var unbundledFile = new File(getTemporaryDir(), "unpacked.jar");
            try (var in = zipFile.getInputStream(versionJarEntry)) {
                Files.copy(in, unbundledFile.toPath());
            }
            try (ZipFile zf = new ZipFile(unbundledFile)) {
                return getFileIndex(zf);
            }
        }

        var result = new HashSet<String>(zipFile.size());

        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                result.add(entry.getName());
            }
        }

        return result;
    }

    private static boolean isResourceEntry(JarEntry entry) {
        return !entry.getName().endsWith(".class")
               && !entry.isDirectory()
               && !entry.getName().equals(JarFile.MANIFEST_NAME)
               && !isSignatureFile(entry.getName());
    }

    private static boolean isSignatureFile(String name) {
        return name.startsWith("META-INF/")
               && (
                       name.endsWith(".SF")
                       || name.endsWith(".RSA")
                       || name.endsWith(".EC")
                       || name.endsWith(".DSA")
               );
    }

    /**
     * Server jars support embedding the actual jar file using an indirection via a version listing at
     * META-INF/versions.list
     * This method will try to read that list and return the path to the actual jar embedded in the bundle jar.
     */
    @Nullable
    private static String readEmbeddedVersionPath(ZipFile zipFile) throws IOException {
        var entry = zipFile.getEntry("META-INF/versions.list");
        if (entry == null) {
            return null;
        }

        var entries = new ArrayList<String>();
        try (var in = zipFile.getInputStream(entry)) {
            for (var line : new String(in.readAllBytes()).split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] pts = line.split("\t");
                if (pts.length != 3)
                    throw new IOException("Invalid file list line: " + line + " in " + zipFile);
                entries.add(pts[2]);
            }
        }

        if (entries.isEmpty()) {
            return null;
        } else if (entries.size() == 1) {
            return "META-INF/versions/" + entries.get(0);
        } else {
            throw new IOException("Version file list contains more than one entry in " + zipFile);
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getOriginalJar();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServerJar();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getMappings();
}
