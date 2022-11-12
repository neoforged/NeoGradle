package net.minecraftforge.gradle.common.runtime.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.gradle.common.extensions.ArtifactDownloaderExtension;
import net.minecraftforge.gradle.common.runtime.tasks.Runtime;
import org.gradle.api.file.RegularFileProperty;
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

@CacheableTask
public abstract class ListLibraries extends Runtime {
    private static final Attributes.Name FORMAT = new Attributes.Name("Bundler-Format");

    @SuppressWarnings("ConstantConditions")
    public ListLibraries() {
        super();

        getServerBundleFile().fileProvider(getRuntimeArguments().map(arguments -> {
            if (!arguments.containsKey("bundle"))
                return null;

            return new File(arguments.get("bundle").get());
        }));
        getOutputFileName().set("libraries.txt");
    }

    @TaskAction
    public void run() throws Exception {
        final File output = ensureFileWorkspaceReady(getOutput());
        try (FileSystem bundleFs = !getServerBundleFile().isPresent() ? null : FileSystems.newFileSystem(getServerBundleFile().get().getAsFile().toPath(), this.getClass().getClassLoader())) {
            Set<String> artifacts;
            if (bundleFs == null) {
                artifacts = listDownloadJsonLibraries();
            } else {
                artifacts = listBundleLibraries(bundleFs);
            }

            Set<File> libraries = new HashSet<>();
            for (String artifact : artifacts) {
                final ArtifactDownloaderExtension downloader = getDownloader().get();
                File lib = downloader.gradle(artifact, false).get();
                libraries.add(lib);
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
            artifacts.add(entry.id);
        }

        return artifacts;
    }

    private Set<String> listDownloadJsonLibraries() throws IOException {
        Gson gson = new Gson();
        Reader reader = new FileReader(getDownloadedVersionJsonFile().getAsFile().get());
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        reader.close();

        // Gather all the libraries
        Set<String> artifacts = new HashSet<>();
        for (JsonElement libElement : json.getAsJsonArray("libraries")) {
            JsonObject library = libElement.getAsJsonObject();
            String name = library.get("name").getAsString();

            if (library.has("downloads")) {
                JsonObject downloads = library.get("downloads").getAsJsonObject();
                if (downloads.has("artifact"))
                    artifacts.add(name);
                if (downloads.has("classifiers"))
                    downloads.get("classifiers").getAsJsonObject().keySet().forEach(cls -> artifacts.add(name + ':' + cls));
            }
        }

        return artifacts;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getServerBundleFile();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDownloadedVersionJsonFile();

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
}
