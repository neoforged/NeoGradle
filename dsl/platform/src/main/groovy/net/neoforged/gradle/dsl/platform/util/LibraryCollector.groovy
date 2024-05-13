package net.neoforged.gradle.dsl.platform.util

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.platform.model.Artifact
import net.neoforged.gradle.dsl.platform.model.Library
import net.neoforged.gradle.dsl.platform.model.LibraryDownload
import net.neoforged.gradle.util.HashFunction
import org.gradle.api.model.ObjectFactory
import org.jetbrains.annotations.Nullable

import java.nio.file.Files

@CompileStatic
class LibraryCollector extends ModuleIdentificationVisitor {

    private final ObjectFactory objectFactory;
    private final List<URI> repositoryUrls

    private final List<Library> libraries = new ArrayList<>();

    LibraryCollector(ObjectFactory objectFactory, List<URI> repoUrl) {
        super(objectFactory);
        this.objectFactory = objectFactory;
        this.repositoryUrls = repoUrl
    }

    @Override
    protected void visitModule(File file, String group, String module, String version, @Nullable String classifier, final String extension) throws Exception {
        final Library library = objectFactory.newInstance(Library.class);
        final LibraryDownload download = objectFactory.newInstance(LibraryDownload.class);
        final Artifact artifact = objectFactory.newInstance(Artifact.class);

        library.getDownload().set(download);
        download.getArtifact().set(artifact);

        final String path = group.replace(".", "/") + "/" + module + "/" + version + "/" + module + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + extension;
        String url = getMavenServerFor(path) + "/" + path;
        int pos = 0
        while (attemptConnection(url) !== 200 && pos < repositoryUrls.size()) {
            url = repositoryUrls.get(pos++).resolve(path).toString()
        }

        final String name = group + ":" + module + ":" + version + (classifier.isEmpty() ? "" : ":" + classifier) + "@" + extension;

        library.getName().set(name);
        try {
            artifact.getPath().set(path);
            artifact.getUrl().set(url);
            artifact.getSha1().set(HashFunction.SHA1.hash(file));
            artifact.getSize().set(Files.size(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        libraries.add(library);
    }

    private static int attemptConnection(String url) {
        try {
            final conn = (HttpURLConnection) url.toURL().openConnection()
            conn.setRequestMethod('HEAD')
            conn.connect()
            int rc = conn.responseCode
            conn.disconnect()
            return rc
        } catch (Exception ignored) {
            return 404
        }
    }

    private static String getMavenServerFor(String path) {
        try {
            final URL mojangMavenUrl = new URL("https://libraries.minecraft.net/" + path);
            final HttpURLConnection connection = (HttpURLConnection) mojangMavenUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            return connection.getResponseCode() == 200 ? "https://libraries.minecraft.net" : "https://maven.neoforged.net/releases";
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return "https://maven.neoforged.net/releases";
        }
    }

    List<Library> getLibraries() {
        return libraries;
    }
}
