package net.neoforged.gradle.neoform.runtime.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.FileReader;
import java.io.Reader;

@CacheableTask
public abstract class DownloadVersionJson extends DownloadFile {

    public DownloadVersionJson() {
        super();

        getDownloadedManifest().finalizeValueOnRead();
    }

    @Override
    public void run() throws Exception {
        if (!getDownloadInfo().isPresent()) {
            Gson gson = new Gson();
            Reader reader = new FileReader(getDownloadedManifest().get().getAsFile());
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            reader.close();

            for (JsonElement e : json.getAsJsonArray("versions")) {
                String v = e.getAsJsonObject().get("id").getAsString();
                if (v.equals(getMinecraftVersion().get().toString())) {
                    final FileDownloadingUtils.DownloadInfo info = new FileDownloadingUtils.DownloadInfo(e.getAsJsonObject().get("url").getAsString(), null, "json", v, null);
                    doDownloadFrom(info);
                    return;
                }
            }

            throw new IllegalStateException("Could not find the correct version json.");
        } else {
            super.run();
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getDownloadedManifest();
}
