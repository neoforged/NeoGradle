package net.minecraftforge.gradle.common.runtime.tasks;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.gradle.common.runtime.tasks.action.DownloadAssetAction;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.common.util.VersionJson;
import net.minecraftforge.gradle.common.util.workers.DefaultWorkerExecutorHelper;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
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
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

@CacheableTask
public abstract class DownloadAssets extends DefaultRuntime implements Runtime {

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

        final DefaultWorkerExecutorHelper executorHelper = getProject().getObjects().newInstance(DefaultWorkerExecutorHelper.class, getWorkerExecutor());

        final DownloadAssetAction assetDownloadAction = createDownloadAction(assetIndexData);
        assetDownloadAction.getOutputFile().set(getAssetIndexFile());

        assetDownloadAction.execute(executorHelper);

        executorHelper.await();
    }

    private void downloadAssets() {
        final AssetIndex assetIndex = Utils.fromJson(getAssetIndexFile().getAsFile().get(), AssetIndex.class);

        final DefaultWorkerExecutorHelper executorHelper = getProject().getObjects().newInstance(DefaultWorkerExecutorHelper.class, getWorkerExecutor());

        assetIndex.getObjects().forEach((assetKey, asset) -> {
            final Provider<File> assetFile = getFileInOutputDirectory(asset.getPath());
            final Provider<String> assetUrl = getAssetRepository()
                    .map(repo -> repo.endsWith("/") ? repo : repo + "/")
                    .map(TransformerUtils.guard(repository -> repository + asset.getPath()));

            final DownloadAssetAction assetDownloadAction = getProject().getObjects().newInstance(DownloadAssetAction.class, this);
            assetDownloadAction.getOutputFile().fileProvider(assetFile);
            assetDownloadAction.getUrl().set(assetUrl);
            assetDownloadAction.getSha1().set(asset.getHash());

            assetDownloadAction.execute(executorHelper);
        });

        executorHelper.await();
    }

    private DownloadAssetAction createDownloadAction(VersionJson.Download download) {
        final DownloadAssetAction action = getProject().getObjects().newInstance(DownloadAssetAction.class, this);
        action.getUrl().set(download.getUrl().toString());
        action.getSha1().set(download.getSha1());
        return action;
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
