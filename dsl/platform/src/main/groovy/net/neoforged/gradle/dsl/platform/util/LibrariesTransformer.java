package net.neoforged.gradle.dsl.platform.util;

import net.neoforged.gradle.dsl.platform.model.Artifact;
import net.neoforged.gradle.dsl.platform.model.Library;
import net.neoforged.gradle.dsl.platform.model.LibraryDownload;
import net.neoforged.gradle.util.HashFunction;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.GradleException;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.problems.Problems;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class LibrariesTransformer {

    private static final URI MOJANG_MAVEN = URI.create(UrlConstants.MOJANG_MAVEN);
    private static final URI NEOFORGED_MAVEN = URI.create(UrlConstants.NEO_FORGE_MAVEN);

    public static Provider<Iterable<? extends Library>> transform(
            ListProperty<ComponentArtifactIdentifier> libraryArtifactIds,
            ListProperty<ResolvedVariantResult> libraryArtifactVariants,
            ListProperty<RegularFile> libraryArtifactFiles,
            //Provider<List<URI>> registryLocations,
            ProjectLayout layout,
            ObjectFactory objectFactory) {

        /*final Provider<List<URI>> prioritizedLocations = registryLocations.map(urls -> {
            final List<URI> prioritizedUrls = new ArrayList<>(urls);
            // Only remote repositories make sense (no maven local)
            prioritizedUrls.removeIf(url -> !url.getScheme().equals("http") && !url.getScheme().equals("https"));
            // Always try Mojang Maven first, then our installer Maven
            prioritizedUrls.removeIf(url -> url.getHost().equals(MOJANG_MAVEN.getHost()));
            prioritizedUrls.removeIf(url -> url.getHost().equals(NEOFORGED_MAVEN.getHost()) && url.getPath().startsWith(NEOFORGED_MAVEN.getPath()));
            prioritizedUrls.add(0, NEOFORGED_MAVEN);
            prioritizedUrls.add(0, MOJANG_MAVEN);

            return prioritizedUrls;
        });*/

        return libraryArtifactVariants.zip(
                libraryArtifactFiles,
                VariantsAndFiles::new
        ).zip(
                libraryArtifactIds,
                new LibraryExtractor(objectFactory)
        );
    }

    record VariantsAndFiles(List<ResolvedVariantResult> variants, List<RegularFile> files) { }

    public static class IdExtractor
            implements Transformer<List<ComponentArtifactIdentifier>, Set<ResolvedArtifactResult>> {
        @Override
        public List<ComponentArtifactIdentifier> transform(Set<ResolvedArtifactResult> artifacts) {
            return artifacts.stream().map(ResolvedArtifactResult::getId).collect(Collectors.toList());
        }
    }

    public static class VariantExtractor implements Transformer<List<ResolvedVariantResult>, Set<ResolvedArtifactResult>> {
        @Override
        public List<ResolvedVariantResult> transform(Set<ResolvedArtifactResult> artifacts) {
            return artifacts.stream().map(ResolvedArtifactResult::getVariant).collect(Collectors.toList());
        }
    }

    public static class FileExtractor implements Transformer<List<RegularFile>, Set<ResolvedArtifactResult>> {
        private final ProjectLayout projectLayout;

        public FileExtractor(ProjectLayout projectLayout) {
            this.projectLayout = projectLayout;
        }

        @Override
        public List<RegularFile> transform(Set<ResolvedArtifactResult> artifacts) {
            Directory projectDirectory = projectLayout.getProjectDirectory();
            return artifacts.stream().map(a -> projectDirectory.file(a.getFile().getAbsolutePath())).collect(Collectors.toList());
        }
    }

    private static class LibraryExtractor implements BiFunction<VariantsAndFiles, List<ComponentArtifactIdentifier>, Iterable<? extends Library>> {

        private final ObjectFactory objectFactory;

        private LibraryExtractor(ObjectFactory objectFactory) {
            this.objectFactory = objectFactory;
        }

        public ObjectFactory getObjectFactory() {
            return objectFactory;
        }

        private static String createPath(ModuleComponentArtifactIdentifier module, String moduleId) {
            //The format is group:name:version:classifier@extension
            final boolean hasClassifier = moduleId.chars().map(c -> (char) c)
                    .filter(c -> c == ':').count() == 3;
            final String extension = moduleId.contains("@") ? moduleId.substring(moduleId.indexOf('@') + 1) : "jar";
            final String classifier = hasClassifier ?
                    moduleId.contains("@") ?
                            moduleId.substring(moduleId.lastIndexOf(':') + 1, moduleId.indexOf('@')) :
                            moduleId.substring(moduleId.lastIndexOf(':') + 1) :
                    "";
            final String group = module.getComponentIdentifier().getGroup();
            final String version = module.getComponentIdentifier().getVersion();
            final String name = module.getComponentIdentifier().getModule();
            return "%s/%s/%s/%s-%s%s.%s".formatted(group.replace(".", "/"), name, version, name, version, classifier.isEmpty() ? "" : "-" + classifier, extension);
        }

        @Override
        public Iterable<? extends Library> apply(VariantsAndFiles variantsAndFiles, List<ComponentArtifactIdentifier> componentArtifactIdentifiers) {
            final List<ResolvedVariantResult> variants = variantsAndFiles.variants();
            final List<RegularFile> files = variantsAndFiles.files();

            final Set<Library> libraries = new HashSet<>();
            for (int i = 0; i < variants.size(); i++) {
                final ComponentArtifactIdentifier id = componentArtifactIdentifiers.get(i);
                if (!(id instanceof ModuleComponentArtifactIdentifier module)) {
                    throw new GradleException("Unsupported artifact identifier: " + id);
                }
                final RegularFile file = files.get(i);

                final Library library = getObjectFactory().newInstance(Library.class);
                final LibraryDownload download = getObjectFactory().newInstance(LibraryDownload.class);
                final Artifact artifact = getObjectFactory().newInstance(Artifact.class);

                library.getDownload().set(download);
                download.getArtifact().set(artifact);

                final String moduleId = module.getComponentIdentifier().getDisplayName();
                final String path = createPath(module, moduleId);

                try {
                    artifact.getSha1().set(HashFunction.SHA1.hash(file.getAsFile()));
                    artifact.getSize().set(Files.size(file.getAsFile().toPath()));
                    artifact.getPath().set(path);
                    artifact.getUrl().set("%s%s".formatted(UrlConstants.NEO_FORGE_MAVEN, path));

                    library.getName().set(module.getComponentIdentifier().getDisplayName());

                    libraries.add(library);
                } catch (IOException e) {
                    throw new GradleException("Failed to get library artifact!", e);
                }
            }
            return libraries;
        }
    }

    private static class UrlResolver implements BiFunction<Iterable<? extends Library>, List<URI>, @Nullable Iterable<? extends Library>> {

        private final HttpClient httpClient = HttpClient.newBuilder().build();
        private final Logger logger;

        private UrlResolver(Problems problem, Logger logger) {
            this.logger = logger;
        }

        @Override
        public @Nullable Iterable<? extends Library> apply(Iterable<? extends Library> libraries, List<URI> uris) {

            final Set<CompletableFuture<Void>> futures = new HashSet<>();
            for (Library library : libraries) {
                final LibraryDownload download = library.getDownload().get();
                final Artifact artifact = download.getArtifact().get();
                final String path = artifact.getPath().get();

                futures.add(CompletableFuture.runAsync(() -> {
                    for (URI uri : uris) {
                        final URI url = joinUris(uri, path);
                        if (url.getScheme().equals("http") || url.getScheme().equals("https")) {

                        }
                    }
                }));
            }

            return libraries;
        }

        private static URI joinUris(URI repositoryUrl, String path) {
            var baseUrl = repositoryUrl.toString();
            if (baseUrl.endsWith("/") && path.startsWith("/")) {
                while (path.startsWith("/")) {
                    path = path.substring(1);
                }
                return URI.create(baseUrl + path);
            } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
                return URI.create(baseUrl + "/" + path);
            } else {
                return URI.create(baseUrl + path);
            }
        }
    }
}
