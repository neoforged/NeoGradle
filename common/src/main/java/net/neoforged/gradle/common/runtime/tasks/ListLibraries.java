package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.hasher.TaskHashingAware;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.util.FileCacheUtils;
import net.neoforged.gradle.util.HashFunction;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@CacheableTask
public abstract class ListLibraries extends DefaultRuntime implements TaskHashingAware
{
    private static final Attributes.Name FORMAT = new Attributes.Name("Bundler-Format");
    
    @SuppressWarnings("ConstantConditions")
    public ListLibraries() {
        super();
        
        getLibrariesDirectory().convention(FileCacheUtils.getLibrariesCacheDirectory(getProject()).map(TransformerUtils.ensureExists()));
        getServerBundleFile().fileProvider(getRuntimeArguments().map(arguments -> {
            if (!arguments.containsKey("bundle"))
                return null;
            
            return new File(arguments.get("bundle").get());
        }));
        getOutputFileName().set("libraries.txt");
        getIsOffline().convention(getProject().getGradle().getStartParameter().isOffline());
    }

    @Override
    public Map<String, Object> getHashableProperties()
    {
        var properties = new HashMap<>(getInputs().getProperties());
        properties.remove("isOffline"); //We don't care about the offline mode!
        return properties;
    }

    @Input
    public abstract Property<Boolean> getIsOffline();

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void run() throws IOException {
        getCacheService().get()
                .cached(
                        this,
                        ICacheableJob.Initial.merging("collect", getLibrariesDirectory(), this::extractAndCollect)
                )
                .withStage(
                        ICacheableJob.Staged.file("list", this::createList, getOutput())
                )
                .execute();
    }

    private Set<File> extractAndCollect() throws IOException {
        try (FileSystem bundleFs = !getServerBundleFile().isPresent() ? null : FileSystems.newFileSystem(getServerBundleFile().get().getAsFile().toPath(), this.getClass().getClassLoader())) {
            final Set<File> libraries;
            if (bundleFs == null) {
                libraries = downloadAndListJsonLibraries();
            } else {
                libraries = unpackAndListBundleLibraries(bundleFs);
            }
            return libraries;
        }
    }

    private Void createList(final Set<File> libraries) throws FileNotFoundException {
        // Write the list
        final File output = ensureFileWorkspaceReady(getOutput());
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8));
        Iterator<File> itr = libraries.stream().sorted(Comparator.comparing(File::getAbsolutePath)).iterator();
        while (itr.hasNext()) {
            writer.println("-e=" + itr.next().getAbsolutePath());
        }
        writer.flush();
        writer.close();

        return null;
    }
    
    private List<FileList.Entry> listBundleLibraries(FileSystem bundleFs) throws IOException {
        Path mfp = bundleFs.getPath("META-INF", "MANIFEST.MF");
        if (!Files.exists(mfp)) {
            throw new RuntimeException("Input archive does not contain META-INF/MANIFEST.MF");
        }
        
        Manifest mf;
        try (InputStream is = Files.newInputStream(mfp)) {
            mf = new Manifest(is);
        }
        String format = mf.getMainAttributes().getValue(FORMAT);
        if (format == null) {
            throw new RuntimeException("Invalid bundler archive; missing format entry from manifest");
        }
        if (!"1.0".equals(format)) {
            throw new RuntimeException("Unsupported bundler format " + format + "; only 1.0 is supported");
        }
        
        return FileList.read(bundleFs.getPath("META-INF", "libraries.list")).entries;
    }

    private Set<File> unpackAndListBundleLibraries(FileSystem bundleFs) throws IOException {
        final File outputDir = getLibrariesDirectory().get().getAsFile();
        
        final List<FileList.Entry> libraryPaths = listBundleLibraries(bundleFs);
        
        return libraryPaths.stream()
                       .map(entry -> {
                           final String path = String.format("META-INF/libraries/%s", entry.path);
                           final File output = new File(outputDir, path);
                           try {
                               if (!output.exists() || !HashFunction.SHA1.hash(output).equalsIgnoreCase(entry.hash)) {
                                   Files.copy(bundleFs.getPath(path), output.toPath());
                               }
                           } catch (IOException e) {
                               throw new UncheckedIOException(e);
                           }
                           return output;
                       }).collect(Collectors.toSet());
    }
    
    private Set<File> downloadAndListJsonLibraries() {
        return getVersionJsonLibraries().getFiles();
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServerBundleFile();

    @InputFiles
    @Optional
    @CompileClasspath
    public abstract ConfigurableFileCollection getVersionJsonLibraries();

    @Override
    public Provider<FileTree> getOutputAsTree()
    {
        return getOutput().map(it -> getObjectFactory().fileTree().from(it));
    }
    
    @OutputDirectory
    public abstract DirectoryProperty getLibrariesDirectory();
    
    private static class FileList {
        
        private final List<FileList.Entry> entries;
        
        private FileList(List<FileList.Entry> entries) {
            this.entries = entries;
        }
        
        static FileList read(Path path) throws IOException {
            List<FileList.Entry> ret = new ArrayList<>();
            
            for (String line : Files.readAllLines(path)) {
                String[] pts = line.split("\t");
                if (pts.length != 3) {
                    throw new IllegalStateException("Invalid file list line: " + line);
                }
                ret.add(new FileList.Entry(pts[0], pts[1], pts[2]));
            }
            
            return new FileList(ret);
        }
        
        private static final class Entry {
            private final String hash;
            private final String id;
            private final String path;
            
            private Entry(String hash, String id, String path) {
                this.hash = hash;
                this.id = id;
                this.path = path;
            }
            
            public String hash() {
                return hash;
            }
            
            public String id() {
                return id;
            }
            
            public String path() {
                return path;
            }
            
            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                final Entry that = (Entry) obj;
                return Objects.equals(this.hash, that.hash) &&
                               Objects.equals(this.id, that.id) &&
                               Objects.equals(this.path, that.path);
            }
            
            @Override
            public int hashCode() {
                return Objects.hash(hash, id, path);
            }
            
            @Override
            public String toString() {
                return "Entry[" +
                               "hash=" + hash + ", " +
                               "id=" + id + ", " +
                               "path=" + path + ']';
            }
            
        }
    }
    
    private static final class PathAndUrl {
        private final String path;
        private final String url;
        private final String hash;
        
        private PathAndUrl(String path, String url, String hash) {
            this.path = path;
            this.url = url;
            this.hash = hash;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getUrl() {
            return url;
        }

        public String getHash() {
            return hash;
        }
    }
}
