package net.neoforged.gradle.neoform.runtime.tasks;

import com.google.gson.JsonObject;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.common.util.SerializationUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class DownloadCore extends DownloadFile {

    public DownloadCore() {
        super();

        getDownloadedVersionJson().finalizeValueOnRead();
        getArtifact().finalizeValueOnRead();
        getExtension().finalizeValueOnRead();
    }

    @Override
    public void run() throws Exception {
        if (!getDownloadInfo().isPresent()) {
            JsonObject json = SerializationUtils.fromJson(getDownloadedVersionJson().get().getAsFile(), JsonObject.class);

            JsonObject artifactInfo = json.getAsJsonObject("downloads").getAsJsonObject(getArtifact().get());
            String url = artifactInfo.get("url").getAsString();
            String hash = artifactInfo.get("sha1").getAsString();
            String version = json.getAsJsonObject().get("id").getAsString();
            final FileDownloadingUtils.DownloadInfo info = new FileDownloadingUtils.DownloadInfo(url, hash, getExtension().get(), version, getArtifact().get());

            doDownloadFrom(info);
        } else {
            super.run();
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getDownloadedVersionJson();

    @Input
    public abstract Property<String> getArtifact();

    @Input
    public abstract Property<String> getExtension();
}
