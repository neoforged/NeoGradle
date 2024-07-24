package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CentralCacheService;
import net.neoforged.gradle.common.util.FileCacheUtils;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.action.DownloadFileAction;
import net.neoforged.gradle.common.util.SerializationUtils;
import net.neoforged.gradle.common.util.VersionJson;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage"})
@CacheableTask
public abstract class DownloadAssets extends DefaultTask implements WithWorkspace {

    private final Provider<Directory> assetsCache;

    public DownloadAssets() {
        this.assetsCache = getAssetsDirectory(getProject(), getVersionJson());
        getAssetIndex().convention("asset-index");
        getAssetIndexFileName().convention(getAssetIndex().map(index -> index + ".json"));
        getAssetIndexTargetFile().convention(getRegularFileInAssetsDirectory(getAssetIndexFileName().map(name -> "indexes/" + name)));
        getAssetIndexFile().convention(getAssetIndexTargetFile());
        getVersionJson().convention(getVersionJsonFile().map(TransformerUtils.guard(file -> VersionJson.get(file.getAsFile()))));
        getAssetRepository().convention("https://resources.download.minecraft.net/");
        getIsOffline().convention(getProject().getGradle().getStartParameter().isOffline());
    }

    public static @NotNull Provider<Directory> getAssetsDirectory(final Project project, final Provider<VersionJson> versionJsonProvider) {
        return FileCacheUtils.getAssetsCacheDirectory(project).flatMap(assetsDir -> assetsDir.dir(versionJsonProvider.map(VersionJson::getId))).map(TransformerUtils.ensureExists());
    }

    protected Provider<File> getFileInAssetsDirectory(final String fileName) {
        return assetsCache.map(directory -> directory.file(fileName).getAsFile());
    }

    protected Provider<RegularFile> getRegularFileInAssetsDirectory(final Provider<String> fileName) {
        return assetsCache.flatMap(directory -> fileName.map(directory::file));
    }

    @ServiceReference(CommonProjectPlugin.ASSETS_SERVICE)
    public abstract Property<CentralCacheService> getAssetsCache();

    @TaskAction
    public void run() {
        downloadAssetIndex();
        downloadAssets();
    }

    private void downloadAssetIndex() {
        final VersionJson json = getVersionJson().get();
        final VersionJson.AssetIndex assetIndexData = json.getAssetIndex();

        final WorkQueue executor = getWorkerExecutor().noIsolation();
        executor.submit(DownloadFileAction.class, params -> {
            params.getUrl().set(assetIndexData.getUrl().toString());
            params.getShouldValidateHash().set(true);
            params.getSha1().set(assetIndexData.getSha1());
            params.getOutputFile().set(getAssetIndexFile());
            params.getIsOffline().set(getIsOffline());
        });

        executor.await();
    }

    private void downloadAssets() {
        final AssetIndex assetIndex = SerializationUtils.fromJson(getAssetIndexFile().getAsFile().get(), AssetIndex.class);

        final WorkQueue executor = getWorkerExecutor().noIsolation();

        assetIndex.getObjects().values().stream().distinct().forEach((asset) -> {
            final Provider<File> assetFile = getFileInAssetsDirectory(String.format("objects%s%s", File.separator, asset.getPath()));
            final Provider<String> assetUrl = getAssetRepository()
                    .map(repo -> repo.endsWith("/") ? repo : repo + "/")
                    .map(TransformerUtils.guard(repository -> repository + asset.getPath()));

            executor.submit(DownloadFileAction.class, params -> {
                params.getIsOffline().set(getIsOffline());
                params.getShouldValidateHash().set(true);
                params.getOutputFile().fileProvider(assetFile);
                params.getUrl().set(assetUrl);
                params.getSha1().set(asset.getHash());
            });
        });

        executor.await();
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionJsonFile();

    @Input
    public abstract Property<VersionJson> getVersionJson();

    @Input
    public abstract Property<String> getAssetIndex();
    
    @Input
    public abstract Property<String> getAssetIndexFileName();

    @Input
    public abstract Property<String> getAssetRepository();

    @Internal
    public abstract RegularFileProperty getAssetIndexTargetFile();

    @OutputFile
    public abstract RegularFileProperty getAssetIndexFile();

    @Input
    public abstract Property<Boolean> getIsOffline();
    
    private static class AssetIndex {
        private Map<String, Asset> objects = Maps.newHashMap();

        public Map<String, Asset> getObjects() {
            return objects;
        }

        public void setObjects(Map<String, Asset> objects) {
            this.objects = objects;
        }
    }

    private static class Asset {
        private String hash;

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getPath() {
            return hash.substring(0, 2) + '/' + hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Asset)) return false;

            Asset asset = (Asset) o;

            return getHash().equals(asset.getHash());
        }

        @Override
        public int hashCode() {
            return getHash().hashCode();
        }
    }
}
