package net.neoforged.gradle.neoform.runtime.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.common.util.SerializationUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class DownloadVersionJson extends DownloadFile {

    public DownloadVersionJson() {
        super();

        getDownloadedManifest().finalizeValueOnRead();
    }

    @Override
    public void run() throws Exception {
        if (!getDownloadInfo().isPresent()) {
            JsonObject json = SerializationUtils.fromJson(getDownloadedManifest().get().getAsFile(), JsonObject.class);

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
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getDownloadedManifest();
}
