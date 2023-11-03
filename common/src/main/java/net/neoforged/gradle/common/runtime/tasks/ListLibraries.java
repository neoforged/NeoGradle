package net.neoforged.gradle.common.runtime.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CentralCacheService;
import net.neoforged.gradle.common.util.FileCacheUtils;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.apache.commons.io.FileUtils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;

import javax.swing.*;
import java.io.*;
import java.net.URL;
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
public abstract class ListLibraries extends DefaultRuntime {
    private static final Attributes.Name FORMAT = new Attributes.Name("Bundler-Format");
    
    @SuppressWarnings("ConstantConditions")
    public ListLibraries() {
        super();
        
        getLibrariesDirectory().convention(FileCacheUtils.getAssetsCacheDirectory(getProject()).map(TransformerUtils.ensureExists()));
        getServerBundleFile().fileProvider(getRuntimeArguments().map(arguments -> {
            if (!arguments.containsKey("bundle"))
                return null;
            
            return new File(arguments.get("bundle").get());
        }));
        getOutputFileName().set("libraries.txt");
    }
    
    @ServiceReference(CommonProjectPlugin.LIBRARIES_SERVICE)
    public abstract Property<CentralCacheService> getLibrariesCache();
    
    @TaskAction
    public void run() throws Exception {
        final File output = ensureFileWorkspaceReady(getOutput());
        try (FileSystem bundleFs = !getServerBundleFile().isPresent() ? null : FileSystems.newFileSystem(getServerBundleFile().get().getAsFile().toPath(), this.getClass().getClassLoader())) {
            final Set<File> libraries;
            if (bundleFs == null) {
                libraries = downloadAndListJsonLibraries();
            } else {
                libraries = unpackAndListBundleLibraries(bundleFs);
            }
            
            // Write the list
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8));
            for (File file : libraries) {
                writer.println("-e=" + file.getAbsolutePath());
            }
            writer.flush();
            writer.close();
        }
    }
    
    private Set<String> listBundleLibraries(FileSystem bundleFs) throws IOException {
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
        
        FileList libraries = FileList.read(bundleFs.getPath("META-INF", "libraries.list"));
        Set<String> artifacts = new HashSet<>();
        for (FileList.Entry entry : libraries.entries) {
            artifacts.add(entry.path);
        }
        
        return artifacts;
    }
    
    private Set<PathAndUrl> listDownloadJsonLibraries() throws IOException {
        Gson gson = new Gson();
        Reader reader = new FileReader(getDownloadedVersionJsonFile().getAsFile().get());
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        reader.close();
        
        // Gather all the libraries
        Set<PathAndUrl> artifacts = new HashSet<>();
        for (JsonElement libElement : json.getAsJsonArray("libraries")) {
            JsonObject library = libElement.getAsJsonObject();
            
            if (library.has("downloads")) {
                JsonObject downloads = library.get("downloads").getAsJsonObject();
                if (downloads.has("artifact")) {
                    final JsonObject artifact = downloads.getAsJsonObject("artifact");
                    artifacts.add(
                            new PathAndUrl(
                                    artifact.get("path").getAsString(),
                                    artifact.get("url").getAsString()
                            )
                    );
                }
            }
        }
        
        return artifacts;
    }
    
    private Set<File> unpackAndListBundleLibraries(FileSystem bundleFs) throws IOException {
        final File outputDir = getLibrariesDirectory().get().getAsFile();
        
        final Set<String> libraryPaths = listBundleLibraries(bundleFs);
        
        return libraryPaths.stream()
                       .map(path -> String.format("META-INF/libraries/%s", path))
                       .map(path -> {
                           final File output = new File(outputDir, path);
                           try (final InputStream stream = Files.newInputStream(bundleFs.getPath(path))) {
                               FileUtils.copyInputStreamToFile(stream, output);
                           } catch (IOException e) {
                               throw new UncheckedIOException(e);
                           }
                           return output;
                       }).collect(Collectors.toSet());
    }
    
    private Set<File> downloadAndListJsonLibraries() throws IOException {
        final Set<PathAndUrl> libraryCoordinates = listDownloadJsonLibraries();
        final File outputDirectory = getLibrariesDirectory().get().getAsFile();
        
        final Set<File> result = new HashSet<>();
        
        for (PathAndUrl libraryCoordinate : libraryCoordinates) {
            final File outputFile = new File(outputDirectory, libraryCoordinate.path);
            FileUtils.copyInputStreamToFile(new URL(libraryCoordinate.url).openStream(), outputFile);
            result.add(outputFile);
        }
        
        return result;
    }
    
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServerBundleFile();
    
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getDownloadedVersionJsonFile();
    
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
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
    
    private final class PathAndUrl {
        private final String path;
        private final String url;
        
        private PathAndUrl(String path, String url) {
            this.path = path;
            this.url = url;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getUrl() {
            return url;
        }
    }
}
