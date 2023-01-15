package net.minecraftforge.gradle.common.runtime.tasks;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.base.util.TransformerUtils;
import net.minecraftforge.gradle.common.runtime.tasks.action.DownloadFileAction;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.common.util.VersionJson;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

@CacheableTask
public abstract class DownloadAssets extends DefaultRuntime {

    public DownloadAssets() {
        getAssetIndexFileName().convention("asset-index.json");
        getAssetIndexFile().convention(getRegularFileInOutputDirectory(getAssetIndexFileName()));
        getVersionJson().convention(getVersionJsonFile().map(TransformerUtils.guard(file -> VersionJson.get(file.getAsFile()))));
        getAssetRepository().convention("https://resources.download.minecraft.net/");
    }

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
            params.getIsOffline().set(getProject().getGradle().getStartParameter().isOffline());
        });

        executor.await();
    }

    private void downloadAssets() {
        final AssetIndex assetIndex = Utils.fromJson(getAssetIndexFile().getAsFile().get(), AssetIndex.class);

        final WorkQueue executor = getWorkerExecutor().noIsolation();
        assetIndex.getObjects().forEach((assetKey, asset) -> {
            final Provider<File> assetFile = getFileInOutputDirectory(asset.getPath());
            final Provider<String> assetUrl = getAssetRepository()
                    .map(repo -> repo.endsWith("/") ? repo : repo + "/")
                    .map(TransformerUtils.guard(repository -> repository + asset.getPath()));

            executor.submit(DownloadFileAction.class, params -> {
                params.getIsOffline().set(getProject().getGradle().getStartParameter().isOffline());
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
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getVersionJsonFile();

    @Input
    public abstract Property<VersionJson> getVersionJson();

    @Input
    public abstract Property<String> getAssetIndexFileName();

    @Input
    public abstract Property<String> getAssetRepository();

    @OutputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getAssetIndexFile();

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
    }
}
