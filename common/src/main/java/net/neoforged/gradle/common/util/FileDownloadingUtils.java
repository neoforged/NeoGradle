package net.neoforged.gradle.common.util;

import net.neoforged.gradle.util.HashFunction;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;

public final class FileDownloadingUtils {

    private FileDownloadingUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileDownloadingUtils. This is a utility class");
    }


    public static void downloadThrowing(Project project, FileDownloadingUtils.DownloadInfo info, File file) {
        try {
            downloadTo(project, info, file);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to download the file from: %s to: %s", info, file), e);
        }
    }

    public static void downloadTo(Project project, DownloadInfo info, File file) throws IOException {
        // Check if file exists in local installer cache
        if (info.type.equals("jar") && info.side.equals("client")) {
            File localPath = new File(getMCDir() + File.separator + "versions" + File.separator + info.version + File.separator + info.version + ".jar");
            if (localPath.exists() && HashFunction.SHA1.hash(localPath).equalsIgnoreCase(info.hash)) {
                org.apache.commons.io.FileUtils.copyFile(localPath, file);
                return;
            }
        }

        if (!project.getGradle().getStartParameter().isOffline()) {
            FileUtils.copyURLToFile(new URL(info.url), file);
        } else if (!file.exists()) {
            throw new RuntimeException("Could not find the file: " + file + " and we are offline.");
        }
    }

    public static File getMCDir()
    {
        switch (VersionJson.OS.getCurrent()) {
            case OSX:
                return new File(System.getProperty("user.home") + "/Library/Application Support/minecraft");
            case WINDOWS:
                return new File(System.getenv("APPDATA") + "\\.minecraft");
            case LINUX:
            default:
                return new File(System.getProperty("user.home") + "/.minecraft");
        }
    }

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
