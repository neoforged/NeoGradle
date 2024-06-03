package net.neoforged.gradle.common.extensions;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.tasks.MinecraftArtifactFileCacheProvider;
import net.neoforged.gradle.common.tasks.MinecraftVersionManifestFileCacheProvider;
import net.neoforged.gradle.common.util.FileCacheUtils;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.dsl.common.util.MinecraftArtifactType;
import net.neoforged.gradle.common.util.SerializationUtils;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.*;
import net.neoforged.gradle.util.HashFunction;
import net.neoforged.gradle.util.UrlConstants;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MinecraftArtifactCacheExtension implements ConfigurableDSLElement<MinecraftArtifactCache>, MinecraftArtifactCache {

    private final Project project;
    private final Map<CacheFileSelector, File> cacheFiles;

    private static final class TaskKey {
        private final Project project;
        private final String gameVersion;
        private final DistributionType type;

        private TaskKey(Project project, String gameVersion, DistributionType type) {
            this.project = project;
            this.gameVersion = gameVersion;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskKey taskKey = (TaskKey) o;
            return Objects.equals(project, taskKey.project) && Objects.equals(gameVersion, taskKey.gameVersion) && type == taskKey.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, gameVersion, type);
        }
    }

    private final Map<TaskKey, Map<GameArtifact, TaskProvider<? extends WithOutput>>> tasks = new ConcurrentHashMap<>();

    @Inject
    public MinecraftArtifactCacheExtension(Project project) {
        this.project = project;
        this.cacheFiles = new ConcurrentHashMap<>();

        //TODO: Move this to gradle user home.
        this.getCacheDirectory().fileProvider(project.provider(() -> new File(project.getGradle().getGradleUserHomeDir(), "caches/minecraft")));
        this.getCacheDirectory().finalizeValueOnRead();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public abstract DirectoryProperty getCacheDirectory();

    @Override
    public final Map<CacheFileSelector, File> getCacheFiles() {
        return ImmutableMap.copyOf(this.cacheFiles);
    }

    @Override
    public final Map<GameArtifact, File> cacheGameVersion(String gameVersion, DistributionType side) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(gameVersion);

        final Map<GameArtifact, File> result = new EnumMap<>(GameArtifact.class);

        GameArtifact.VERSION_MANIFEST.doWhenRequired(side, () -> result.put(GameArtifact.VERSION_MANIFEST, this.cacheVersionManifest(resolvedVersion)));
        GameArtifact.CLIENT_JAR.doWhenRequired(side, () -> result.put(GameArtifact.CLIENT_JAR, this.cacheVersionArtifact(resolvedVersion, DistributionType.CLIENT)));
        GameArtifact.SERVER_JAR.doWhenRequired(side, () -> result.put(GameArtifact.SERVER_JAR, this.cacheVersionArtifact(resolvedVersion, DistributionType.SERVER)));
        GameArtifact.CLIENT_MAPPINGS.doWhenRequired(side, () -> result.put(GameArtifact.CLIENT_MAPPINGS, this.cacheVersionMappings(resolvedVersion, DistributionType.CLIENT)));
        GameArtifact.SERVER_MAPPINGS.doWhenRequired(side, () -> result.put(GameArtifact.SERVER_MAPPINGS, this.cacheVersionMappings(resolvedVersion, DistributionType.SERVER)));

        return result;
    }

    @Override
    @NotNull
    public final Map<GameArtifact, TaskProvider<? extends WithOutput>> cacheGameVersionTasks(final Project project, String gameVersion, final DistributionType side) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(gameVersion);

        final TaskKey key = new TaskKey(project, resolvedVersion.getVersion(), side);

        return tasks.computeIfAbsent(key, k -> {
            final Map<GameArtifact, TaskProvider<? extends WithOutput>> results = new EnumMap<>(GameArtifact.class);

            final TaskProvider<MinecraftVersionManifestFileCacheProvider> manifest = FileCacheUtils.createVersionManifestFileCacheProvidingTask(project, resolvedVersion);

            GameArtifact.VERSION_MANIFEST.doWhenRequired(side, () -> results.put(GameArtifact.VERSION_MANIFEST, manifest));
            GameArtifact.CLIENT_JAR.doWhenRequired(side, () -> results.put(GameArtifact.CLIENT_JAR, FileCacheUtils.createArtifactFileCacheProvidingTask(project, resolvedVersion.getVersion(), DistributionType.CLIENT, MinecraftArtifactType.EXECUTABLE, manifest, results.values())));
            GameArtifact.SERVER_JAR.doWhenRequired(side, () -> results.put(GameArtifact.SERVER_JAR, FileCacheUtils.createArtifactFileCacheProvidingTask(project, resolvedVersion.getVersion(), DistributionType.SERVER, MinecraftArtifactType.EXECUTABLE, manifest, results.values())));
            GameArtifact.CLIENT_MAPPINGS.doWhenRequired(side, () -> results.put(GameArtifact.CLIENT_MAPPINGS, FileCacheUtils.createArtifactFileCacheProvidingTask(project, resolvedVersion.getVersion(), DistributionType.CLIENT, MinecraftArtifactType.MAPPINGS, manifest, results.values())));
            GameArtifact.SERVER_MAPPINGS.doWhenRequired(side, () -> results.put(GameArtifact.SERVER_MAPPINGS, FileCacheUtils.createArtifactFileCacheProvidingTask(project, resolvedVersion.getVersion(), DistributionType.SERVER, MinecraftArtifactType.MAPPINGS, manifest, results.values())));

            return results;
        });
    }

    @NotNull
    @Override
    public NamedDomainObjectProvider<? extends WithOutput> gameArtifactTask(@NotNull NamedDomainObjectCollection<WithOutput> tasks, @NotNull GameArtifact artifact, @NotNull final String minecraftVersion) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(minecraftVersion);

        if (artifact == GameArtifact.VERSION_MANIFEST) {
            return tasks.named(NamingConstants.Task.CACHE_VERSION_MANIFEST + resolvedVersion.getVersion(), MinecraftVersionManifestFileCacheProvider.class);
        }

        final String taskName = "%s%s%s%s".formatted(
                NamingConstants.Task.CACHE_VERSION_PREFIX,
                StringUtils.capitalize(artifact.getType().orElseThrow().name().toLowerCase()),
                StringUtils.capitalize(artifact.getDistributionType().orElseThrow().getName().toLowerCase()),
                resolvedVersion.getVersion());

        return tasks.named(taskName, MinecraftArtifactFileCacheProvider.class);
    }

    @Override
    public final File cacheLauncherMetadata() {
        return this.cache(UrlConstants.MOJANG_MANIFEST, CacheFileSelector.launcherMetadata());
    }

    @Override
    public final File cacheVersionManifest(String gameVersion) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(gameVersion);

        return this.cacheVersionManifest(resolvedVersion);
    }

    public final File cacheVersionManifest(MinecraftVersionAndUrl resolvedVersion) {
        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJson(resolvedVersion.getVersion());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionManifestToCache(project, getCacheDirectory().get().getAsFile(), resolvedVersion.getVersion()));
    }

    @Override
    public final File cacheVersionArtifact(String gameVersion, DistributionType side) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(gameVersion);

        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJar(resolvedVersion.getVersion(), side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionArtifactToCache(project, getCacheDirectory().get().getAsFile(), resolvedVersion.getVersion(), side));
    }

    public final File cacheVersionArtifact(MinecraftVersionAndUrl resolvedVersion, DistributionType side) {
        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJar(resolvedVersion.getVersion(), side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionArtifactToCache(project, getCacheDirectory().get().getAsFile(), resolvedVersion.getVersion(), side));
    }

    @Override
    public final File cacheVersionMappings(@NotNull String gameVersion, DistributionType side) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(gameVersion);

        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionMappings(resolvedVersion.getVersion(), side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionMappingsToCache(project, getCacheDirectory().get().getAsFile(), resolvedVersion.getVersion(), side));
    }

    public final File cacheVersionMappings(@NotNull MinecraftVersionAndUrl resolvedVersion, DistributionType side) {
        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionMappings(resolvedVersion.getVersion(), side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionMappingsToCache(project, getCacheDirectory().get().getAsFile(), resolvedVersion.getVersion(), side));
    }

    @Override
    public final File cache(final URL url, final CacheFileSelector selector) {
        return this.cache(url.toString(), selector);
    }

    @Override
    public final File cache(final String url, final CacheFileSelector selector) {
        return this.cacheFiles.computeIfAbsent(selector, cacheKey -> downloadJsonToCache(project, url, getCacheDirectory().getAsFile().get(), selector));
    }

    private File downloadVersionManifestToCache(Project project, final File cacheDirectory, final String minecraftVersion) {
        final File manifestFile = new File(new File(cacheDirectory, CacheFileSelector.launcherMetadata().getCacheDirectory()), CacheFileSelector.launcherMetadata().getCacheFileName());

        String url = null;

        JsonObject json = SerializationUtils.fromJson(manifestFile, JsonObject.class);

        for (JsonElement e : json.getAsJsonArray("versions")) {
            String v = e.getAsJsonObject().get("id").getAsString();
            if (Objects.equals(minecraftVersion, "+") || v.equals(minecraftVersion)) {
                url = e.getAsJsonObject().get("url").getAsString();
                break;
            }
        }

        if (url == null) {
            throw new IllegalStateException("Could not find the correct version json for version: " + minecraftVersion);
        }

        return downloadJsonToCache(project, url, cacheDirectory, CacheFileSelector.forVersionJson(minecraftVersion));
    }

    private File downloadVersionArtifactToCache(final Project project, final File cacheDirectory, String minecraftVersion, final DistributionType side) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(minecraftVersion);

        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                resolvedVersion.getVersion(),
                side.getName(),
                CacheFileSelector.forVersionJar(resolvedVersion.getVersion(), side.getName()),
                String.format("Failed to download game artifact %s for %s", side.getName(), resolvedVersion.getVersion()));
    }

    private File downloadVersionMappingsToCache(final Project project, final File cacheDirectory, String minecraftVersion, final DistributionType side) {
        final MinecraftVersionAndUrl resolvedVersion = resolveVersion(minecraftVersion);

        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                resolvedVersion.getVersion(),
                String.format("%s_mappings", side.getName()),
                CacheFileSelector.forVersionMappings(resolvedVersion.getVersion(), side.getName()),
                String.format("Failed to download game mappings of %s for %s", side.getName(), resolvedVersion.getVersion()));
    }

    private File doDownloadVersionDownloadToCache(Project project, File cacheDirectory, String minecraftVersion, final String artifact, final CacheFileSelector cacheFileSelector, final String potentialError) {
        final MinecraftVersionAndUrl minecraftVersionAndUrl = resolveVersion(minecraftVersion);

        final File versionManifestFile = this.cacheVersionManifest(minecraftVersionAndUrl);

        try {
            JsonObject json = SerializationUtils.fromJson(versionManifestFile, JsonObject.class);

            JsonObject artifactInfo = json.getAsJsonObject("downloads").getAsJsonObject(artifact);
            String url = artifactInfo.get("url").getAsString();
            String hash = artifactInfo.get("sha1").getAsString();
            String version = json.getAsJsonObject().get("id").getAsString();

            final FileDownloadingUtils.DownloadInfo info = new FileDownloadingUtils.DownloadInfo(url, hash, "jar", version, artifact);

            final File cacheFile = new File(new File(cacheDirectory, cacheFileSelector.getCacheDirectory()), cacheFileSelector.getCacheFileName());

            if (cacheFile.exists()) {
                final String fileHash = HashFunction.SHA1.hash(cacheFile);
                if (fileHash.equals(hash)) {
                    return cacheFile;
                }
            }

            FileDownloadingUtils.downloadTo(project.getGradle().getStartParameter().isOffline(), info, cacheFile);
            return cacheFile;
        } catch (IOException e) {
            throw new RuntimeException(potentialError, e);
        }
    }

    private File downloadJsonToCache(Project project, final String url, final File cacheDirectory, final CacheFileSelector selector) {
        final File cacheFile = new File(new File(cacheDirectory, selector.getCacheDirectory()), selector.getCacheFileName());
        downloadJsonTo(project, url, cacheFile);
        return cacheFile;
    }

    private void downloadJsonTo(Project project, String url, File file) {
        FileDownloadingUtils.downloadThrowing(project.getGradle().getStartParameter().isOffline(), new FileDownloadingUtils.DownloadInfo(url, null, "json", null, null), file);
    }

    @Override
    public MinecraftVersionAndUrl resolveVersion(final String gameVersion) {
        final File launcherMetadata = this.cacheLauncherMetadata();

        JsonObject json = SerializationUtils.fromJson(launcherMetadata, JsonObject.class);

        for (JsonElement e : json.getAsJsonArray("versions")) {
            if (gameVersion.equals("+") || e.getAsJsonObject().get("id").getAsString().equals(gameVersion)) {
                return new MinecraftVersionAndUrl(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject().get("url").getAsString());
            }
        }

        throw new IllegalStateException("Could not find the correct version json.");
    }

    public Provider<MinecraftVersionAndUrl> resolveVersion(Provider<String> gameVersion) {
        return gameVersion.map(this::resolveVersion);
    }
}
