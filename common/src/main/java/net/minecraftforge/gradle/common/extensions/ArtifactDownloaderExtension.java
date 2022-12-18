package net.minecraftforge.gradle.common.extensions;

import com.google.common.collect.ImmutableMap;
import groovy.util.Node;
import groovy.xml.XmlParser;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.artifactural.gradle.GradleRepositoryAdapter;
import net.minecraftforge.gradle.common.util.Artifact;
import net.minecraftforge.gradle.common.util.DownloadUtils;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.common.util.Utils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.AuthenticationSupported;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.credentials.Credentials;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.provider.Provider;
import org.gradle.authentication.http.BasicAuthentication;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Extension which handles multiple downloads of artifacts, including generating them if needed.
 * This is based of the MavenArtifactDownloader in FG5 but is now project specific.
 */
@SuppressWarnings("unused") //Part of the public API of FG.
public abstract class ArtifactDownloaderExtension {

    private final Map<DownloadKey, CompletableFuture<File>> inProgress = new ConcurrentHashMap<>();
    private final Map<String, String> versions = new HashMap<>();
    private final Project project;
    private final ArtifactProviderExtension artifactProvider;
    private int counter = 0;

    @Inject
    public ArtifactDownloaderExtension(final Project project) {
        this.project = project;

        this.artifactProvider = this.project.getExtensions().getByType(ArtifactProviderExtension.class);
    }

    @NotNull
    public Provider<File> download(String artifact, boolean changing) {
        return _download(artifact, changing, true, true);
    }

    @NotNull
    public Provider<String> getVersion(String notation) {
        return this.artifactProvider.from(notation)
                .map(artifact -> {
                    if (!artifact.getVersion().endsWith("+") && !artifact.isSnapshot())
                        return artifact.getVersion();

                    //noinspection ConstantConditions - We are allowed to return null from the transformer.
                    return null;
                })
                .orElse(_versionFromDownload(notation));
    }

    @NotNull
    private Provider<String> _versionFromDownload(final String notation) {
        return _download(notation, true, false, true)
                .map(file -> versions.get(notation));
    }

    @NotNull
    public Provider<File> gradle(String artifact, boolean changing) {
        return _download(artifact, changing, false, true);
    }

    @NotNull
    public Provider<File> generate(String artifact, boolean changing) {
        return _download(artifact, changing, true, false);
    }

    @NotNull
    public Provider<File> manual(String artifact, boolean changing) {
        return _download(artifact, changing, false, false);
    }

    @NotNull
    private Provider<File> _download(String artifact, boolean changing, boolean generated, boolean gradle) {
        final DownloadKey key = new DownloadKey(artifact, changing, generated, gradle, true);
        return project.provider(() -> inProgress.get(key))
                .map(inProgressDownload -> {
                    project.getLogger().info("Download of {} is already in progress. Awaiting completion...", artifact);
                    return inProgressDownload.join();
                })
                .orElse(_doDownload(key));
    }

    private Provider<File> _doDownload(final DownloadKey key) {
        Artifact artifact = Artifact.from(key.artifactNotation());
        List<MavenArtifactRepository> mavens = new ArrayList<>();
        List<GradleRepositoryAdapter> fakes = new ArrayList<>();
        List<ArtifactRepository> others = new ArrayList<>();

        project.getRepositories().forEach(repo -> {
            if (repo instanceof MavenArtifactRepository)
                mavens.add((MavenArtifactRepository) repo);
            else if (repo instanceof GradleRepositoryAdapter)
                fakes.add((GradleRepositoryAdapter) repo);
            else
                others.add(repo);
        });

        final CompletableFuture<File> activeDownload = new CompletableFuture<>();
        return project.provider(() -> key)
                .map(TransformerUtils.guard(
                        downloadKey -> {
                            File result = null;
                            if (downloadKey.generated()) {
                                result = _generate(fakes, artifact);
                            }

                            if (result == null && downloadKey.manual()) {
                                result = _manual(mavens, artifact, key.changing());
                            }

                            if (result == null && downloadKey.gradle()) {
                                result = _gradle(others, artifact, key.changing());
                            }

                            //noinspection ConstantConditions - We are allowed to return null from the transformer.
                            return result;
                        },
                        () -> inProgress.put(key, activeDownload),
                        file -> inProgress.remove(key, activeDownload),
                        e -> {
                            activeDownload.completeExceptionally(e);
                            project.getLogger().info("Failed to download artifactNotation: {}", key.artifactNotation(), e);
                        },
                        () -> inProgress.remove(key, activeDownload)
                ));
    }

    @NotNull
    private File _generate(List<GradleRepositoryAdapter> repos, Artifact artifact) {
        for (GradleRepositoryAdapter repo : repos) {
            File ret = repo.getArtifact(artifact);
            if (ret != null && ret.exists())
                return ret;
        }
        return null;
    }

    @Nullable
    private File _manual(List<MavenArtifactRepository> repos, Artifact artifact, boolean changing) throws IOException, URISyntaxException {
        if (!artifact.getVersion().endsWith("+") && !artifact.isSnapshot()) {
            for (MavenArtifactRepository repo : repos) {
                Pair<Artifact, File> pair = _manualMaven(repo, repo.getUrl(), artifact, changing);
                if (pair != null && pair.getValue().exists())
                    return pair.getValue();
            }
            return null;
        }

        List<Pair<Artifact, File>> versions = new ArrayList<>();

        // Gather list of all versions from all repos.
        for (MavenArtifactRepository repo : repos) {
            Pair<Artifact, File> pair = _manualMaven(repo, repo.getUrl(), artifact, changing);
            if (pair != null && pair.getValue().exists())
                versions.add(pair);
        }

        Artifact version = null;
        File ret = null;
        for (Pair<Artifact, File> ver : versions) {
            //Select highest version
            if (version == null || version.compareTo(ver.getKey()) < 0) {
                version = ver.getKey();
                ret = ver.getValue();
            }
        }

        if (ret == null)
            return null;

        this.versions.put(artifact.getDescriptor(), version.getVersion());
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Pair<Artifact, File> _manualMaven(@Nullable AuthenticationSupported auth, URI maven, Artifact artifact, boolean changing) throws IOException, URISyntaxException {
        if (artifact.getVersion().endsWith("+")) {
            //I THINK +'s are only valid in the end version, So 1.+ and not 1.+.4 as that'd make no sense.
            //It also appears you can't do something like 1.5+ to NOT get 1.4/1.3. So.. mimic that.
            File meta = _downloadWithCache(auth, maven, artifact.getGroup().replace('.', '/') + '/' + artifact.getName() + "/maven-metadata.xml", true, true);
            if (meta == null)
                return null; //Don't error, other repos might have it.
            try {
                Node xml = new XmlParser().parse(meta);
                Node versioning = getPath(xml, "versioning/versions");
                List<Node> versions = versioning == null ? null : (List<Node>) versioning.get("version");
                if (versions == null) {
                    meta.delete();
                    throw new IOException("Invalid maven-metadata.xml file, missing version list");
                }
                String prefix = artifact.getVersion().substring(0, artifact.getVersion().length() - 1); // Trim +
                ArtifactVersion minVersion = (!prefix.endsWith(".") && prefix.length() > 0) ? new DefaultArtifactVersion(prefix) : null;
                if (minVersion != null) { //Support min version like 1.5+ by saving it, and moving the prefix
                    //minVersion = new DefaultArtifactVersion(prefix);
                    int idx = prefix.lastIndexOf('.');
                    prefix = idx == -1 ? "" : prefix.substring(0, idx + 1);
                }
                final String prefix_ = prefix;
                ArtifactVersion highest = versions.stream().map(Node::text)
                        .filter(s -> s.startsWith(prefix_))
                        .map(DefaultArtifactVersion::new)
                        .filter(v -> minVersion == null || minVersion.compareTo(v) <= 0)
                        .sorted()
                        .reduce((first, second) -> second).orElse(null);
                if (highest == null)
                    return null; //We have no versions that match what we want, so move on to next repo.
                artifact = Artifact.from(artifact.getGroup(), artifact.getName(), highest.toString(), artifact.getClassifier(), artifact.getExtension());
            } catch (SAXException | ParserConfigurationException e) {
                meta.delete();
                throw new IOException("Invalid maven-metadata.xml file", e);
            }
        } else if (artifact.getVersion().contains("-SNAPSHOT")) {
            return null; //TODO
            //throw new IllegalArgumentException("Snapshot versions are not supported, yet... " + artifactNotation.getDescriptor());
        }

        File ret = _downloadWithCache(auth, maven, artifact.getPath(), changing, false);
        return ret == null ? null : ImmutablePair.of(artifact, ret);
    }

    //I'm sure there is a better way but not sure at the moment
    @SuppressWarnings("unchecked")
    @Nullable
    private Node getPath(Node node, String path) {
        Node tmp = node;
        for (String pt : path.split("/")) {
            tmp = ((List<Node>) tmp.get(pt)).stream().findFirst().orElse(null);
            if (tmp == null)
                return null;
        }
        return tmp;
    }

    @Nullable
    private File _gradle(List<ArtifactRepository> repos, Artifact mine, boolean changing) {
        String name = "mavenDownloader_" + mine.getDescriptor().replace(':', '_');
        name += "_" + counter++;

        //Remove old repos, and only use the ones we're told to.
        List<ArtifactRepository> old = new ArrayList<>(project.getRepositories());
        project.getRepositories().clear();
        project.getRepositories().addAll(repos);

        Configuration cfg = project.getConfigurations().create(name);
        ExternalModuleDependency dependency = (ExternalModuleDependency) project.getDependencies().create(mine.getDescriptor());
        dependency.setChanging(changing);
        cfg.getDependencies().add(dependency);
        cfg.resolutionStrategy(strat -> {
            strat.cacheChangingModulesFor(5, TimeUnit.MINUTES);
            strat.cacheDynamicVersionsFor(5, TimeUnit.MINUTES);
        });
        Set<File> files;
        try {
            files = cfg.resolve();
        } catch (NullPointerException npe) {
            // This happens for unknown reasons deep in Gradle code... so we SHOULD find a way to fix it, but
            //honestly i'd rather deprecate this whole system and replace it with downloading things ourselves.
            project.getLogger().error("Failed to download " + mine.getDescriptor() + " gradle exploded");
            return null;
        }
        File ret = files.iterator().next(); //We only want the first, not transitive

        cfg.getResolvedConfiguration().getResolvedArtifacts().forEach(art -> {
            ModuleVersionIdentifier resolved = art.getModuleVersion().getId();
            if (resolved.getGroup().equals(mine.getGroup()) && resolved.getName().equals(mine.getName())) {
                if ((mine.getClassifier() == null && art.getClassifier() == null) || mine.getClassifier().equals(art.getClassifier()))
                    this.versions.put(mine.getDescriptor(), resolved.getVersion());
            }
        });

        project.getConfigurations().remove(cfg);

        project.getRepositories().clear(); //Clear the repos so we can re-add in the correct oder.
        project.getRepositories().addAll(old); //Readd all the normal repos.
        return ret;
    }

    @Nullable
    private File _downloadWithCache(@Nullable AuthenticationSupported auth, URI maven, String path, boolean changing, boolean bypassLocal) throws IOException, URISyntaxException {
        URL url = new URIBuilder(maven)
                .setPath(maven.getPath() + '/' + path)
                .build()
                .normalize()
                .toURL();
        File target = Utils.getCache(project, "maven_downloader", path);

        Map<String, String> headers = null;
        if (auth != null && !auth.getAuthentication().isEmpty() && auth.getAuthentication().stream().anyMatch(a -> a instanceof BasicAuthentication)) {
            // We use this to prevent an IllegalStateException with getCredentials() if non-password credentials are used.
            Credentials credentials = auth.getCredentials(Credentials.class);
            if (credentials instanceof PasswordCredentials) {
                final PasswordCredentials passwordCredentials = (PasswordCredentials) credentials;
                headers = ImmutableMap.of(
                        "Authorization", "Basic " + Base64.getEncoder().encodeToString((passwordCredentials.getUsername() + ":" + passwordCredentials.getPassword()).getBytes(StandardCharsets.UTF_8))
                );
            }
        }

        return DownloadUtils.downloadWithCache(url, target, headers, changing, bypassLocal);
    }


    /**
     * Key used to track active downloads and avoid downloading the same file in two threads concurrently,
     * leading to corrupted files on disk.
     */
    private static final class DownloadKey {
        private final String artifactNotation;
        private final boolean changing;
        private final boolean generated;
        private final boolean gradle;
        private final boolean manual;

        /**
         *
         */
        private DownloadKey(String artifactNotation, boolean changing, boolean generated, boolean gradle,
                            boolean manual) {
            this.artifactNotation = artifactNotation;
            this.changing = changing;
            this.generated = generated;
            this.gradle = gradle;
            this.manual = manual;
        }

        public String artifactNotation() {
            return artifactNotation;
        }

        public boolean changing() {
            return changing;
        }

        public boolean generated() {
            return generated;
        }

        public boolean gradle() {
            return gradle;
        }

        public boolean manual() {
            return manual;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final DownloadKey that = (DownloadKey) obj;
            return Objects.equals(this.artifactNotation, that.artifactNotation) &&
                    this.changing == that.changing &&
                    this.generated == that.generated &&
                    this.gradle == that.gradle &&
                    this.manual == that.manual;
        }

        @Override
        public int hashCode() {
            return Objects.hash(artifactNotation, changing, generated, gradle, manual);
        }

        @Override
        public String toString() {
            return "DownloadKey[" +
                    "artifactNotation=" + artifactNotation + ", " +
                    "changing=" + changing + ", " +
                    "generated=" + generated + ", " +
                    "gradle=" + gradle + ", " +
                    "manual=" + manual + ']';
        }

    }
}
