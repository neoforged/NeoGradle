package net.neoforged.gradle.dsl.platform.util

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.platform.model.Artifact
import net.neoforged.gradle.dsl.platform.model.Library
import net.neoforged.gradle.dsl.platform.model.LibraryDownload
import net.neoforged.gradle.util.HashFunction
import org.apache.commons.io.FilenameUtils
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.jetbrains.annotations.Nullable

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.function.Function

@CompileStatic
class LibraryCollector extends ModuleIdentificationVisitor {

    private static final URI MOJANG_MAVEN = URI.create("https://libraries.minecraft.net")
    private static final URI NEOFORGED_MAVEN = URI.create("https://maven.neoforged.net/releases")

    private final ObjectFactory objectFactory;
    private final List<URI> repositoryUrls

    private final List<Future<Library>> libraries = new ArrayList<>();

    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final Logger logger

    LibraryCollector(ObjectFactory objectFactory, List<URI> repoUrl, Logger logger) {
        super(objectFactory);
        this.logger = logger
        this.objectFactory = objectFactory;
        this.repositoryUrls = new ArrayList<>(repoUrl)

        // Only remote repositories make sense (no maven local)
        repositoryUrls.removeIf { it.scheme.toLowerCase() != "https" && it.scheme.toLowerCase() != "http" }
        // Always try Mojang Maven first, then our installer Maven
        repositoryUrls.removeIf { it.host == MOJANG_MAVEN.host }
        repositoryUrls.removeIf { it.host == NEOFORGED_MAVEN.host && it.path.startsWith(NEOFORGED_MAVEN.path) }
        repositoryUrls.add(0, NEOFORGED_MAVEN)
        repositoryUrls.add(0, MOJANG_MAVEN)

        logger.info("Collecting libraries from:")
        for (var repo in repositoryUrls) {
            logger.info(" - $repo")
        }
    }

    void visit(ResolvedArtifactResult artifactResult) {
        def componentId = artifactResult.id.componentIdentifier
        if (componentId instanceof ModuleComponentIdentifier) {
            visitModule(
                    artifactResult.file,
                    componentId.getGroup(),
                    componentId.getModule(),
                    componentId.getVersion(),
                    guessMavenClassifier(artifactResult.file, componentId),
                    FilenameUtils.getExtension(artifactResult.file.name)
            )
        } else {
            logger.warn("Cannot handle component: " + componentId)
        }
    }

    static String guessMavenClassifier(File file, ModuleComponentIdentifier id) {
        var artifact = id.module
        var version = id.version
        var expectedBasename = artifact + "-" + version;
        var filename = file.name
        var startOfExt = filename.lastIndexOf('.');
        if (startOfExt != -1) {
            filename = filename.substring(0, startOfExt);
        }

        if (filename.startsWith(expectedBasename + "-")) {
            return filename.substring((expectedBasename + "-").length());
        }
        return ""
    }

    @Override
    protected void visitModule(File file, String group, String module, String version, @Nullable String classifier, final String extension) throws Exception {
        final Library library = objectFactory.newInstance(Library.class);
        final LibraryDownload download = objectFactory.newInstance(LibraryDownload.class);
        final Artifact artifact = objectFactory.newInstance(Artifact.class);

        library.getDownload().set(download);
        download.getArtifact().set(artifact);

        final String name = group + ":" + module + ":" + version + (classifier.isEmpty() ? "" : ":" + classifier) + "@" + extension;
        final String path = group.replace(".", "/") + "/" + module + "/" + version + "/" + module + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + extension;

        library.getName().set(name);
        try {
            artifact.getPath().set(path);
            artifact.getSha1().set(HashFunction.SHA1.hash(file));
            artifact.getSize().set(Files.size(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Try each configured repository in-order to find the file
        CompletableFuture<Library> libraryFuture = null;
        for (var repositoryUrl in repositoryUrls) {
            def artifactUri = joinUris(repositoryUrl, path)
            var request = HttpRequest.newBuilder(artifactUri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build()

            Function<String, CompletableFuture<Library>> makeRequest = (String previousError) -> {
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .thenApply { response ->
                            if (response.statusCode() != 200) {
                                logger.info("  Got ${response.statusCode()} for ${artifactUri}")
                                String message = "Could not find ${artifactUri}: ${response.statusCode()}"
                                // Prepend error message from previous repo if they all fail
                                if (previousError != null) {
                                    message = previousError + "\n" + message
                                }
                                throw new RuntimeException(message)
                            }
                            logger.info("  Found $name -> $artifactUri")
                            artifact.getUrl().set(artifactUri.toString());
                            library
                        }
            };

            if (libraryFuture == null) {
                libraryFuture = makeRequest(null)
            } else {
                libraryFuture = libraryFuture.exceptionallyCompose { error ->
                    makeRequest(error.getMessage())
                }
            }
        }

        libraries.add(libraryFuture);
    }

    private static URI joinUris(URI repositoryUrl, String path) {
        var baseUrl = repositoryUrl.toString()
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            while (path.startsWith("/")) {
                path = path.substring(1)
            }
            return URI.create(baseUrl + path)
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return URI.create(baseUrl + "/" + path)
        } else {
            return URI.create(baseUrl + path)
        }
    }

    Set<Library> getLibraries() {
        var result = libraries.collect {
            it.get()
        }
        logger.info("Collected ${result.size()} libraries")
        return new HashSet<>(result)
    }
}
