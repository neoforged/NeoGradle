package net.neoforged.gradle.common.runtime.tasks;

import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        final File originalJar = getOriginalJar().get().getAsFile();
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
            manifest.getMainAttributes().putValue("NeoForm-Minecraft-Dists", "server client");

            addSourceDistEntries(clientFiles, serverFiles, "client", mappings, manifest);
            addSourceDistEntries(serverFiles, clientFiles, "server", mappings, manifest);

            try (var jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputJar)), manifest)) {
                // Generally ignore directories, manifests and class files
                var clientEntries = clientZip.entries();
                while (clientEntries.hasMoreElements()) {
                    var clientEntry = clientEntries.nextElement();
                    if (isResourceEntry(clientEntry)) {
                        jos.putNextEntry(clientEntry);
                        try (var clientIn = clientZip.getInputStream(clientEntry)) {
                            clientIn.transferTo(jos);
                        }
                        jos.closeEntry();
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
                fileAttr.putValue("NeoForm-Minecraft-Dist", dist);

                if (mappings != null && file.endsWith(".class")) {
                    file = mappings.remapClass(file.substring(0, file.length() - ".class".length())) + ".class";
                }
                manifest.getEntries().put(file, fileAttr);
            }
        }
    }

    private static Set<String> getFileIndex(ZipFile zipFile) {
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
