package net.neoforged.gradle.common.extensions;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.tasks.MinecraftLauncherFileCacheProvider;
import net.neoforged.gradle.common.tasks.MinecraftVersionManifestFileCacheProvider;
import net.neoforged.gradle.common.util.FileCacheUtils;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.common.util.MinecraftArtifactType;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.HashFunction;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MinecraftArtifactCacheExtension implements ConfigurableDSLElement<MinecraftArtifactCache>, MinecraftArtifactCache {

    private final Project project;
    private final Map<CacheFileSelector, File> cacheFiles;

    private static final class TaskKey{
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
        gameVersion = resolveVersion(gameVersion);

        final Map<GameArtifact, File> result = new EnumMap<>(GameArtifact.class);

        final String finalGameVersion = gameVersion;

        GameArtifact.LAUNCHER_MANIFEST.doWhenRequired(side, () -> result.put(GameArtifact.LAUNCHER_MANIFEST, this.cacheLauncherMetadata()));
        GameArtifact.LAUNCHER_MANIFEST.doWhenRequired(side, () -> result.put(GameArtifact.LAUNCHER_MANIFEST, this.cacheLauncherMetadata()));
        GameArtifact.VERSION_MANIFEST.doWhenRequired(side, () -> result.put(GameArtifact.VERSION_MANIFEST, this.cacheVersionManifest(finalGameVersion)));
        GameArtifact.CLIENT_JAR.doWhenRequired(side, () -> result.put(GameArtifact.CLIENT_JAR, this.cacheVersionArtifact(finalGameVersion, DistributionType.CLIENT)));
        GameArtifact.SERVER_JAR.doWhenRequired(side, () -> result.put(GameArtifact.SERVER_JAR, this.cacheVersionArtifact(finalGameVersion, DistributionType.SERVER)));
        GameArtifact.CLIENT_MAPPINGS.doWhenRequired(side, () -> result.put(GameArtifact.CLIENT_MAPPINGS, this.cacheVersionMappings(finalGameVersion, DistributionType.CLIENT)));
        GameArtifact.SERVER_MAPPINGS.doWhenRequired(side, () -> result.put(GameArtifact.SERVER_MAPPINGS, this.cacheVersionMappings(finalGameVersion, DistributionType.SERVER)));

        return result;
    }

    @Override
    @NotNull
    public final Map<GameArtifact, TaskProvider<? extends WithOutput>> cacheGameVersionTasks(final Project project, String gameVersion, final DistributionType side) {
        gameVersion = resolveVersion(gameVersion);

        final TaskKey key = new TaskKey(project, gameVersion, side);

        final String finalGameVersion = gameVersion;

        return tasks.computeIfAbsent(key, k -> {
            final Map<GameArtifact, TaskProvider<? extends WithOutput>> results = new EnumMap<>(GameArtifact.class);

            final TaskProvider<MinecraftLauncherFileCacheProvider> launcher = FileCacheUtils.createLauncherMetadataFileCacheProvidingTask(project);
            final TaskProvider<MinecraftVersionManifestFileCacheProvider> manifest = FileCacheUtils.createVersionManifestFileCacheProvidingTask(project, finalGameVersion, launcher);
            
            GameArtifact.LAUNCHER_MANIFEST.doWhenRequired(side, () -> results.put(GameArtifact.LAUNCHER_MANIFEST, launcher));
            GameArtifact.VERSION_MANIFEST.doWhenRequired(side, () -> results.put(GameArtifact.VERSION_MANIFEST, manifest));
            GameArtifact.CLIENT_JAR.doWhenRequired(side, () -> results.put(GameArtifact.CLIENT_JAR, FileCacheUtils.createArtifactFileCacheProvidingTask(project, finalGameVersion, DistributionType.CLIENT, MinecraftArtifactType.EXECUTABLE, manifest, results.values())));
            GameArtifact.SERVER_JAR.doWhenRequired(side, () -> results.put(GameArtifact.SERVER_JAR, FileCacheUtils.createArtifactFileCacheProvidingTask(project, finalGameVersion, DistributionType.SERVER, MinecraftArtifactType.EXECUTABLE, manifest, results.values())));
            GameArtifact.CLIENT_MAPPINGS.doWhenRequired(side, () -> results.put(GameArtifact.CLIENT_MAPPINGS, FileCacheUtils.createArtifactFileCacheProvidingTask(project, finalGameVersion, DistributionType.CLIENT, MinecraftArtifactType.MAPPINGS, manifest, results.values())));
            GameArtifact.SERVER_MAPPINGS.doWhenRequired(side, () -> results.put(GameArtifact.SERVER_MAPPINGS, FileCacheUtils.createArtifactFileCacheProvidingTask(project, finalGameVersion, DistributionType.SERVER, MinecraftArtifactType.MAPPINGS, manifest, results.values())));

            return results;
        });
    }

    @Override
    public final File cacheLauncherMetadata() {
        return this.cache(UrlConstants.MOJANG_MANIFEST, CacheFileSelector.launcherMetadata());
    }

    @Override
    public final File cacheVersionManifest(String gameVersion) {
        gameVersion = resolveVersion(gameVersion);

        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJson(gameVersion);
        final String finalGameVersion = gameVersion;
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionManifestToCache(project, getCacheDirectory().get().getAsFile(), finalGameVersion));
    }

    @Override
    public final File cacheVersionArtifact(String gameVersion, DistributionType side) {
        gameVersion = resolveVersion(gameVersion);

        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJar(gameVersion, side.getName());
        final String finalGameVersion = gameVersion;
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionArtifactToCache(project, getCacheDirectory().get().getAsFile(), finalGameVersion, side));
    }

    @Override
    public final File cacheVersionMappings(String gameVersion, DistributionType side) {
        gameVersion = resolveVersion(gameVersion);

        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionMappings(gameVersion, side.getName());
        final String finalGameVersion = gameVersion;
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionMappingsToCache(project, getCacheDirectory().get().getAsFile(), finalGameVersion, side));
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

        Gson gson = new Gson();
        String url = null;
        try(final Reader reader = new FileReader(manifestFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            for (JsonElement e : json.getAsJsonArray("versions")) {
                String v = e.getAsJsonObject().get("id").getAsString();
                if (Objects.equals(minecraftVersion, "+") || v.equals(minecraftVersion)) {
                    url = e.getAsJsonObject().get("url").getAsString();
                    break;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not read the launcher manifest", e);
        }

        if (url == null) {
            throw new IllegalStateException("Could not find the correct version json for version: " + minecraftVersion);
        }

        return downloadJsonToCache(project, url, cacheDirectory, CacheFileSelector.forVersionJson(minecraftVersion));
    }

    private File downloadVersionArtifactToCache(final Project project, final File cacheDirectory, String minecraftVersion, final DistributionType side) {
        minecraftVersion = resolveVersion(minecraftVersion);

        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                minecraftVersion,
                side.getName(),
                CacheFileSelector.forVersionJar(minecraftVersion, side.getName()),
                String.format("Failed to download game artifact %s for %s", side.getName(), minecraftVersion));
    }

    private File downloadVersionMappingsToCache(final Project project, final File cacheDirectory, String minecraftVersion, final DistributionType side) {
        minecraftVersion = resolveVersion(minecraftVersion);

        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                minecraftVersion,
                String.format("%s_mappings", side.getName()),
                CacheFileSelector.forVersionMappings(minecraftVersion, side.getName()),
                String.format("Failed to download game mappings of %s for %s", side.getName(), minecraftVersion));
    }

    private File doDownloadVersionDownloadToCache(Project project, File cacheDirectory, String minecraftVersion, final String artifact, final CacheFileSelector cacheFileSelector, final String potentialError) {
        minecraftVersion = resolveVersion(minecraftVersion);

        final File versionManifestFile = this.cacheVersionManifest(minecraftVersion);

        try {
            Gson gson = new Gson();
            Reader reader = new FileReader(versionManifestFile);
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            reader.close();

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
    public String resolveVersion(final String gameVersion) {
        if (!Objects.equals(gameVersion, "+"))
            return gameVersion;

        final File launcherMetadata = this.cacheLauncherMetadata();

        Gson gson = new Gson();
        String url = null;
        try(final Reader reader = new FileReader(launcherMetadata)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            for (JsonElement e : json.getAsJsonArray("versions")) {
                return e.getAsJsonObject().get("id").getAsString();
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not read the launcher manifest", e);
        }

        throw new IllegalStateException("Could not find the correct version json.");
    }
}
