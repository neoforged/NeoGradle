package net.minecraftforge.gradle.common.extensions;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.gradle.base.util.ConfigurableObject;
import net.minecraftforge.gradle.base.util.GameArtifactUtils;
import net.minecraftforge.gradle.base.util.UrlConstants;
import net.minecraftforge.gradle.common.tasks.FileCacheProviding;
import net.minecraftforge.gradle.common.util.FileDownloadingUtils;
import net.minecraftforge.gradle.dsl.base.util.CacheFileSelector;
import net.minecraftforge.gradle.dsl.base.util.DistributionType;
import net.minecraftforge.gradle.dsl.base.util.GameArtifact;
import net.minecraftforge.gradle.dsl.base.util.NamingConstants;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

import static net.minecraftforge.gradle.common.util.FileDownloadingUtils.downloadThrowing;

public abstract class MinecraftArtifactCacheExtension extends ConfigurableObject<MinecraftArtifactCache> implements MinecraftArtifactCache {

    private final Project project;
    private final Map<CacheFileSelector, File> cacheFiles;

    private static final class TaskKey{
        private final Project project;
        private final String gameVersion;

        private TaskKey(Project project, String gameVersion) {
            this.project = project;
            this.gameVersion = gameVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TaskKey)) return false;

            TaskKey taskKey = (TaskKey) o;

            if (!Objects.equals(project, taskKey.project)) return false;
            return Objects.equals(gameVersion, taskKey.gameVersion);
        }

        @Override
        public int hashCode() {
            int result = project != null ? project.hashCode() : 0;
            result = 31 * result + (gameVersion != null ? gameVersion.hashCode() : 0);
            return result;
        }
    }
    private final Map<TaskKey, Map<GameArtifact, TaskProvider<? extends WithOutput>>> tasks = new ConcurrentHashMap<>();

    @Inject
    public MinecraftArtifactCacheExtension(Project project) {
        this.project = project;
        this.cacheFiles = new ConcurrentHashMap<>();

        this.getCacheDirectory().fileProvider(project.provider(() -> new File(project.getGradle().getGradleHomeDir(), "caches/minecraft")));
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
    public final Map<GameArtifact, File> cacheGameVersion(final String gameVersion, DistributionType side) {
        final Map<GameArtifact, File> result = new EnumMap<>(GameArtifact.class);

        GameArtifactUtils.doWhenRequired(GameArtifact.LAUNCHER_MANIFEST, side, () -> result.put(GameArtifact.LAUNCHER_MANIFEST, this.cacheLauncherMetadata()));
        GameArtifactUtils.doWhenRequired(GameArtifact.VERSION_MANIFEST, side, () -> result.put(GameArtifact.VERSION_MANIFEST, this.cacheVersionManifest(gameVersion)));
        GameArtifactUtils.doWhenRequired(GameArtifact.CLIENT_JAR, side, () -> result.put(GameArtifact.CLIENT_JAR, this.cacheVersionArtifact(gameVersion, DistributionType.CLIENT)));
        GameArtifactUtils.doWhenRequired(GameArtifact.SERVER_JAR, side, () -> result.put(GameArtifact.SERVER_JAR, this.cacheVersionArtifact(gameVersion, DistributionType.SERVER)));
        GameArtifactUtils.doWhenRequired(GameArtifact.CLIENT_MAPPINGS, side, () -> result.put(GameArtifact.CLIENT_MAPPINGS, this.cacheVersionMappings(gameVersion, DistributionType.CLIENT)));
        GameArtifactUtils.doWhenRequired(GameArtifact.SERVER_MAPPINGS, side, () -> result.put(GameArtifact.SERVER_MAPPINGS, this.cacheVersionMappings(gameVersion, DistributionType.SERVER)));

        return result;
    }

    @Override
    @NotNull
    public final Map<GameArtifact, TaskProvider<? extends WithOutput>> cacheGameVersionTasks(final Project project, final File outputDirectory, final String gameVersion, final DistributionType side) {
        final TaskKey key = new TaskKey(project, gameVersion);
        return tasks.computeIfAbsent(key, k -> {
            final Map<GameArtifact, TaskProvider<? extends WithOutput>> results = new EnumMap<>(GameArtifact.class);

            GameArtifactUtils.doWhenRequired(GameArtifact.LAUNCHER_MANIFEST, side, () -> results.put(GameArtifact.LAUNCHER_MANIFEST, this.createFileCacheEntryProvidingTask(project, NamingConstants.Task.CACHE_LAUNCHER_METADATA, gameVersion, outputDirectory, CacheFileSelector.launcherMetadata(), this::cacheLauncherMetadata)));
            GameArtifactUtils.doWhenRequired(GameArtifact.VERSION_MANIFEST, side, () -> results.put(GameArtifact.VERSION_MANIFEST, this.createFileCacheEntryProvidingTask(project, NamingConstants.Task.CACHE_VERSION_MANIFEST, gameVersion, outputDirectory, CacheFileSelector.forVersionJson(gameVersion), () -> this.cacheVersionManifest(gameVersion))));
            GameArtifactUtils.doWhenRequired(GameArtifact.CLIENT_JAR, side, () -> results.put(GameArtifact.CLIENT_JAR, this.createFileCacheEntryProvidingTask(project, NamingConstants.Task.CACHE_VERSION_ARTIFACT_CLIENT, gameVersion, outputDirectory, CacheFileSelector.forVersionJar(gameVersion, DistributionType.CLIENT.getName()), () -> this.cacheVersionArtifact(gameVersion, DistributionType.CLIENT))));
            GameArtifactUtils.doWhenRequired(GameArtifact.SERVER_JAR, side, () -> results.put(GameArtifact.SERVER_JAR, this.createFileCacheEntryProvidingTask(project, NamingConstants.Task.CACHE_VERSION_ARTIFACT_SERVER, gameVersion, outputDirectory, CacheFileSelector.forVersionJar(gameVersion, DistributionType.SERVER.getName()), () -> this.cacheVersionArtifact(gameVersion, DistributionType.SERVER))));
            GameArtifactUtils.doWhenRequired(GameArtifact.CLIENT_MAPPINGS, side, () -> results.put(GameArtifact.CLIENT_MAPPINGS, this.createFileCacheEntryProvidingTask(project, NamingConstants.Task.CACHE_VERSION_MAPPINGS_CLIENT, gameVersion, outputDirectory, CacheFileSelector.forVersionMappings(gameVersion, DistributionType.CLIENT.getName()), () -> this.cacheVersionMappings(gameVersion, DistributionType.CLIENT))));
            GameArtifactUtils.doWhenRequired(GameArtifact.SERVER_MAPPINGS, side, () -> results.put(GameArtifact.SERVER_MAPPINGS, this.createFileCacheEntryProvidingTask(project, NamingConstants.Task.CACHE_VERSION_MAPPINGS_SERVER, gameVersion, outputDirectory, CacheFileSelector.forVersionMappings(gameVersion, DistributionType.SERVER.getName()), () -> this.cacheVersionMappings(gameVersion, DistributionType.SERVER))));

            return results;
        });
    }

    @SuppressWarnings("Convert2Lambda") // Task actions can not be lambdas.
    @NotNull
    private TaskProvider<FileCacheProviding> createFileCacheEntryProvidingTask(final Project project, final String name, final String gameVersion, final File outputDirectory, final CacheFileSelector selector, final Runnable action) {
        return project.getTasks().register(String.format("%s%s", name, gameVersion), FileCacheProviding.class, task -> {
            task.doFirst(new Action<Task>() {
                @Override
                public void execute(Task task) {
                    action.run();
                }
            });
            task.getOutput().fileValue(new File(outputDirectory, selector.getCacheFileName()));
            task.getFileCache().set(getCacheDirectory());
            task.getSelector().set(selector);
            task.setDescription("Retrieves: " + selector.getCacheFileName() + " from the central cache.");
        });
    }

    @Override
    public final File cacheLauncherMetadata() {
        return this.cache(UrlConstants.MOJANG_MANIFEST, CacheFileSelector.launcherMetadata());
    }

    @Override
    public final File cacheVersionManifest(String gameVersion) {
        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJson(gameVersion);
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionManifestToCache(project, getCacheDirectory().get().getAsFile(), gameVersion));
    }

    @Override
    public final File cacheVersionArtifact(String gameVersion, DistributionType side) {
        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionJar(gameVersion, side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionArtifactToCache(project, getCacheDirectory().get().getAsFile(), gameVersion, side));
    }

    @Override
    public final File cacheVersionMappings(String gameVersion, DistributionType side) {
        final CacheFileSelector cacheFileSelector = CacheFileSelector.forVersionMappings(gameVersion, side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionMappingsToCache(project, getCacheDirectory().get().getAsFile(), gameVersion, side));
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
        final File manifestFile = new File(cacheDirectory, CacheFileSelector.launcherMetadata().getCacheFileName());

        Gson gson = new Gson();
        String url = null;
        try(final Reader reader = new FileReader(manifestFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            for (JsonElement e : json.getAsJsonArray("versions")) {
                String v = e.getAsJsonObject().get("id").getAsString();
                if (v.equals(minecraftVersion)) {
                    url = e.getAsJsonObject().get("url").getAsString();
                    break;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not read the launcher manifest", e);
        }

        if (url == null) {
            throw new IllegalStateException("Could not find the correct version json.");
        }

        return downloadJsonToCache(project, url, cacheDirectory, CacheFileSelector.forVersionJson(minecraftVersion));
    }

    private File downloadVersionArtifactToCache(final Project project, final File cacheDirectory, final String minecraftVersion, final DistributionType side) {
        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                minecraftVersion,
                side.getName(),
                CacheFileSelector.forVersionJar(minecraftVersion, side.getName()),
                String.format("Failed to download game artifact %s for %s", side.getName(), minecraftVersion));
    }

    private File downloadVersionMappingsToCache(final Project project, final File cacheDirectory, final String minecraftVersion, final DistributionType side) {
        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                minecraftVersion,
                String.format("%s_mappings", side.getName()),
                CacheFileSelector.forVersionMappings(minecraftVersion, side.getName()),
                String.format("Failed to download game mappings of %s for %s", side.getName(), minecraftVersion));
    }

    private File doDownloadVersionDownloadToCache(Project project, File cacheDirectory, String minecraftVersion, final String artifact, final CacheFileSelector cacheFileSelector, final String potentialError) {
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

            final File cacheFile = new File(cacheDirectory, cacheFileSelector.getCacheFileName());
            FileDownloadingUtils.downloadTo(project, info, cacheFile);
            return cacheFile;
        } catch (IOException e) {
            throw new RuntimeException(potentialError, e);
        }
    }

    private File downloadJsonToCache(Project project, final String url, final File cacheDirectory, final CacheFileSelector selector) {
        final File cacheFile = new File(cacheDirectory, selector.getCacheFileName());
        downloadJsonTo(project, url, cacheFile);
        return cacheFile;
    }

    private void downloadJsonTo(Project project, String url, File file) {
        downloadThrowing(project, new FileDownloadingUtils.DownloadInfo(url, null, "json", null, null), file);
    }
}
