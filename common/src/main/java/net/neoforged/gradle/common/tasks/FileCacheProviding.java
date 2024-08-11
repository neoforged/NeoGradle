package net.neoforged.gradle.common.tasks;

import com.google.gson.JsonObject;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.common.util.SerializationUtils;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import net.neoforged.gradle.util.HashFunction;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;

@DisableCachingByDefault(because = "This is an abstract underlying task which provides defaults and systems for caching game artifacts.")
public abstract class FileCacheProviding extends NeoGradleBase implements WithOutput, WithWorkspace {

    protected FileCacheProviding() {
        this.getFileCache().set(getSelector().map(selector -> getLayout().getProjectDirectory().dir(".gradle/caches/minecraft").dir(selector.getCacheDirectory())));
        this.getFileCache().finalizeValueOnRead();
        
        this.getIsOffline().set(getProject().getGradle().getStartParameter().isOffline());
        
        this.getOutputFileName().set(getSelector().map(CacheFileSelector::getCacheFileName));
        this.getOutput().set(getFileCache().flatMap(cacheDir -> getOutputFileName().map(cacheDir::file)));
    }
    
    @Internal
    public abstract DirectoryProperty getFileCache();

    @Nested
    public abstract Property<CacheFileSelector> getSelector();

    @Optional
    @Input
    public abstract Property<Boolean> getIsOffline();
    
    protected void downloadJsonTo(String url) {
        final File output = getOutput().get().getAsFile();
        FileDownloadingUtils.DownloadInfo info = new FileDownloadingUtils.DownloadInfo(url, null, "json", null, null);
        boolean didWork = FileDownloadingUtils.downloadThrowing(getIsOffline().get(), info, output);
        setDidWork(didWork);
    }
    
    protected File doDownloadVersionDownloadToCache(final String artifact, final String potentialError, File versionManifest) {
        JsonObject json = SerializationUtils.fromJson(versionManifest, JsonObject.class);

        final JsonObject artifactInfo = json.getAsJsonObject("downloads").getAsJsonObject(artifact);
        final String url = artifactInfo.get("url").getAsString();
        final String hash = artifactInfo.get("sha1").getAsString();
        final String version = json.getAsJsonObject().get("id").getAsString();

        final FileDownloadingUtils.DownloadInfo info = new FileDownloadingUtils.DownloadInfo(url, hash, "jar", version, artifact);

        final File output = getOutput().get().getAsFile();
        try {
            if (output.exists()) {
                final String fileHash = HashFunction.SHA1.hash(output);
                if (fileHash.equals(hash)) {
                    return output;
                }
            }

            FileDownloadingUtils.downloadTo(getIsOffline().get(), info, output);
            return output;
        } catch (IOException e) {
            throw new RuntimeException(potentialError, e);
        }
    }
}
