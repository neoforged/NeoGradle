package net.minecraftforge.gradle.common.extensions;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.common.util.*;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

import javax.inject.Inject;
import java.io.*;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraftforge.gradle.common.util.FileDownloadingUtils.downloadThrowing;

public abstract class MinecraftArtifactCacheExtension extends GroovyObjectSupport implements IConfigurableObject<MinecraftArtifactCacheExtension> {

    private final Project project;
    private final Map<ICacheFileSelector, File> cacheFiles;

    @Inject
    public MinecraftArtifactCacheExtension(Project project) {
        this.project = project;
        this.cacheFiles = new ConcurrentHashMap<>();

        this.getCacheDirectory().fileProvider(project.provider(() -> new File(project.getGradle().getGradleHomeDir(), "caches/minecraft")));
        this.getCacheDirectory().finalizeValueOnRead();
    }

    public abstract DirectoryProperty getCacheDirectory();

    public final Map<ICacheFileSelector, File> getCacheFiles() {
        return ImmutableMap.copyOf(this.cacheFiles);
    }

    public final Map<GameArtifact, File> cacheGameVersion(final String gameVersion, ArtifactSide side) {
        final Map<GameArtifact, File> result = new EnumMap<>(GameArtifact.class);

        GameArtifactUtils.doWhenRequired(GameArtifact.LAUNCHER_MANIFEST, side, () -> result.put(GameArtifact.LAUNCHER_MANIFEST, this.cacheLauncherMetadata()));
        GameArtifactUtils.doWhenRequired(GameArtifact.VERSION_MANIFEST, side, () -> result.put(GameArtifact.VERSION_MANIFEST, this.cacheVersionManifest(gameVersion)));
        GameArtifactUtils.doWhenRequired(GameArtifact.CLIENT_JAR, side, () -> result.put(GameArtifact.CLIENT_JAR, this.cacheVersionArtifact(gameVersion, ArtifactSide.CLIENT)));
        GameArtifactUtils.doWhenRequired(GameArtifact.SERVER_JAR, side, () -> result.put(GameArtifact.SERVER_JAR, this.cacheVersionArtifact(gameVersion, ArtifactSide.SERVER)));
        GameArtifactUtils.doWhenRequired(GameArtifact.CLIENT_MAPPINGS, side, () -> result.put(GameArtifact.CLIENT_MAPPINGS, this.cacheVersionMappings(gameVersion, ArtifactSide.CLIENT)));
        GameArtifactUtils.doWhenRequired(GameArtifact.SERVER_MAPPINGS, side, () -> result.put(GameArtifact.SERVER_MAPPINGS, this.cacheVersionMappings(gameVersion, ArtifactSide.SERVER)));

        return result;
    }

    public final File cacheLauncherMetadata() {
        return this.cache(UrlConstants.MOJANG_MANIFEST, ICacheFileSelector.launcherMetadata());
    }

    public final File cacheVersionManifest(String gameVersion) {
        final ICacheFileSelector cacheFileSelector = ICacheFileSelector.forVersionJson(gameVersion);
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionManifestToCache(project, getCacheDirectory().get().getAsFile(), gameVersion));
    }

    public final File cacheVersionArtifact(String gameVersion, ArtifactSide side) {
        final ICacheFileSelector cacheFileSelector = ICacheFileSelector.forVersionJar(gameVersion, side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionArtifactToCache(project, getCacheDirectory().get().getAsFile(), gameVersion, side));
    }

    public final File cacheVersionMappings(String gameVersion, ArtifactSide side) {
        final ICacheFileSelector cacheFileSelector = ICacheFileSelector.forVersionMappings(gameVersion, side.getName());
        return this.cacheFiles.computeIfAbsent(cacheFileSelector, selector -> downloadVersionMappingsToCache(project, getCacheDirectory().get().getAsFile(), gameVersion, side));
    }

    public final File cache(final URL url, final ICacheFileSelector selector) {
        return this.cache(url.toString(), selector);
    }

    public final File cache(final String url, final ICacheFileSelector selector) {
        return this.cacheFiles.computeIfAbsent(selector, cacheKey -> downloadJsonToCache(project, url, getCacheDirectory().getAsFile().get(), selector));
    }

    private File downloadVersionManifestToCache(Project project, final File cacheDirectory, final String minecraftVersion) {
        final File manifestFile = new File(cacheDirectory, ICacheFileSelector.launcherMetadata().getCacheFileName());

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

        return downloadJsonToCache(project, url, cacheDirectory, ICacheFileSelector.forVersionJson(minecraftVersion));
    }

    private File downloadVersionArtifactToCache(final Project project, final File cacheDirectory, final String minecraftVersion, final ArtifactSide side) {
        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                minecraftVersion,
                side.getName(),
                ICacheFileSelector.forVersionJar(minecraftVersion, side.getName()),
                String.format("Failed to download game artifact %s for %s", side.getName(), minecraftVersion));
    }

    private File downloadVersionMappingsToCache(final Project project, final File cacheDirectory, final String minecraftVersion, final ArtifactSide side) {
        return doDownloadVersionDownloadToCache(project,
                cacheDirectory,
                minecraftVersion,
                String.format("%s_mappings", side.getName()),
                ICacheFileSelector.forVersionMappings(minecraftVersion, side.getName()),
                String.format("Failed to download game mappings of %s for %s", side.getName(), minecraftVersion));
    }

    private File doDownloadVersionDownloadToCache(Project project, File cacheDirectory, String minecraftVersion, final String artifact, final ICacheFileSelector cacheFileSelector, final String potentialError) {
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

    private File downloadJsonToCache(Project project, final String url, final File cacheDirectory, final ICacheFileSelector selector) {
        final File cacheFile = new File(cacheDirectory, selector.getCacheFileName());
        downloadJsonTo(project, url, cacheFile);
        return cacheFile;
    }

    private void downloadJsonTo(Project project, String url, File file) {
        downloadThrowing(project, new FileDownloadingUtils.DownloadInfo(url, null, "json", null, null), file);
    }
}
