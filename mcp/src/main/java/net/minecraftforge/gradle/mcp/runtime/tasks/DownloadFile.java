package net.minecraftforge.gradle.mcp.runtime.tasks;

import groovy.cli.Option;
import net.minecraftforge.gradle.common.util.HashFunction;
import net.minecraftforge.gradle.common.util.Utils;
import org.apache.commons.io.FileUtils;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

@CacheableTask
public abstract class DownloadFile extends McpRuntime {

    public DownloadFile() {
        getDownloadInfo().finalizeValueOnRead();
    }

    @TaskAction
    public void run() throws Exception {
        if (getDownloadInfo().isPresent()) {
            final DownloadInfo info = getDownloadInfo().get();
            doDownloadFrom(info);
        } else {
            throw new IllegalStateException("No download info provided");
        }
    }

    protected void doDownloadFrom(DownloadInfo info) throws IOException {
        final File outputFile = ensureFileWorkspaceReady(getOutput());

        // Check if file exists in local installer cache
        if (info.type.equals("jar") && info.side.equals("client")) {
            File localPath = new File(Utils.getMCDir() + File.separator + "versions" + File.separator + info.version + File.separator + info.version + ".jar");
            if (localPath.exists() && HashFunction.SHA1.hash(localPath).equalsIgnoreCase(info.hash)) {
                FileUtils.copyFile(localPath, outputFile);
            } else {
                FileUtils.copyURLToFile(new URL(info.url), outputFile);
            }
        } else {
            FileUtils.copyURLToFile(new URL(info.url), outputFile);
        }

        setDidWork(true);
    }

    @Nested
    @Optional
    public abstract Property<DownloadInfo> getDownloadInfo();

    public static class DownloadInfo implements Serializable {
        private String url;
        private String hash;
        private String type;
        private String version;
        private String side;

        public DownloadInfo(String url, @Nullable String hash, String type, @Nullable String version, @Nullable String side) {
            this.url = url;
            this.hash = hash;
            this.type = type;
            this.version = version;
            this.side = side;
        }

        @Input
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Input
        @Optional
        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        @Input
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Input
        @Optional
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Input
        @Optional
        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }
    }
}
