package net.minecraftforge.gradle.mcp.runtime.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.FileReader;
import java.io.Reader;

@CacheableTask
public abstract class DownloadCoreTask extends DownloadFileTask {

    public DownloadCoreTask() {
        super();

        getDownloadedVersionJson().finalizeValueOnRead();
        getArtifact().finalizeValueOnRead();
        getExtension().finalizeValueOnRead();
    }

    @Override
    public void run() throws Exception {
        if (!getDownloadInfo().isPresent()) {
            Gson gson = new Gson();
            Reader reader = new FileReader(getDownloadedVersionJson().get().getAsFile());
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            reader.close();

            JsonObject artifactInfo = json.getAsJsonObject("downloads").getAsJsonObject(getArtifact().get());
            String url = artifactInfo.get("url").getAsString();
            String hash = artifactInfo.get("sha1").getAsString();
            String version = json.getAsJsonObject().get("id").getAsString();
            final DownloadInfo info = new DownloadInfo(url, hash, getExtension().get(), version, getArtifact().get());

            doDownloadFrom(info);
        } else {
            super.run();
        }
    }

    @Input
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDownloadedVersionJson();

    @Input
    public abstract Property<String> getArtifact();

    @Input
    public abstract Property<String> getExtension();
}
