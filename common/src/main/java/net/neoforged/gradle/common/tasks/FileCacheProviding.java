package net.neoforged.gradle.common.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import net.neoforged.gradle.util.HashFunction;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

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
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLauncherJson();
    
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getVersionManifest();
    
    @Optional
    @Input
    public abstract Property<Boolean> getIsOffline();
    
    protected void downloadJsonTo(String url) {
        final File output = getOutput().get().getAsFile();
        FileDownloadingUtils.downloadThrowing(getIsOffline().get(), new FileDownloadingUtils.DownloadInfo(url, null, "json", null, null), output);
    }
    
    protected void doDownloadVersionDownloadToCache(final String artifact, final String potentialError, File versionManifest) {
        try(final Reader reader = new FileReader(versionManifest)) {
            final File output = getOutput().get().getAsFile();
            
            final Gson gson = new Gson();
            final JsonObject json = gson.fromJson(reader, JsonObject.class);

            final JsonObject artifactInfo = json.getAsJsonObject("downloads").getAsJsonObject(artifact);
            final String url = artifactInfo.get("url").getAsString();
            final String hash = artifactInfo.get("sha1").getAsString();
            final String version = json.getAsJsonObject().get("id").getAsString();
            
            final FileDownloadingUtils.DownloadInfo info = new FileDownloadingUtils.DownloadInfo(url, hash, "jar", version, artifact);
            
            if (output.exists()) {
                final String fileHash = HashFunction.SHA1.hash(output);
                if (fileHash.equals(hash)) {
                    return;
                }
            }
            
            FileDownloadingUtils.downloadTo(getIsOffline().get(), info, output);
        } catch (IOException e) {
            throw new RuntimeException(potentialError, e);
        }
    }
    
    protected static String resolveVersion(final String gameVersion, final File launcherMetadata) {
        if (!Objects.equals(gameVersion, "+"))
            return gameVersion;
        
        Gson gson = new Gson();
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
