package net.neoforged.gradle.common.util;

import net.neoforged.gradle.util.HashFunction;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

public final class FileDownloadingUtils {

    private FileDownloadingUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileDownloadingUtils. This is a utility class");
    }


    public static boolean downloadThrowing(boolean isOffline, FileDownloadingUtils.DownloadInfo info, File file) {
        try {
            return downloadTo(isOffline, info, file);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to download the file from: %s to: %s", info.url, file), e);
        }
    }

    /**
     * @return True if a file was downloaded, false if the file was already up-to-date.
     */
    public static boolean downloadTo(boolean isOffline, DownloadInfo info, File file) throws IOException {
        // Check if file exists in local installer cache
        if (info.type.equals("jar") && info.side.equals("client")) {
            File localPath = new File(getMCDir() + File.separator + "versions" + File.separator + info.version + File.separator + info.version + ".jar");
            if (localPath.exists() && HashFunction.SHA1.hash(localPath).equalsIgnoreCase(info.hash)) {
                org.apache.commons.io.FileUtils.copyFile(localPath, file);
                return true;
            }
        }

        if (!isOffline) {
            return copyURLToFileIfNewer(new URL(info.url), file.toPath());
        } else if (!file.exists()) {
            throw new RuntimeException("Could not find the file: " + file + " and we are offline.");
        } else {
            return false; // We're offline and the file exists
        }
    }

    /**
     * Downloads a file, but attempts to make a conditional request to only re-download if the file has been
     * changed on the remote-server.
     */
    private static boolean copyURLToFileIfNewer(URL url, Path target) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try {
            // Do a Conditional If-Modified-Since request
            if (Files.isRegularFile(target)) {
                FileTime lastModified = Files.getLastModifiedTime(target);
                urlConnection.setIfModifiedSince(lastModified.toMillis());

                // Accessing the response code will cause the request to be sent
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    // Double-Check here -> If the server also returns a last-modified date,
                    // and that is different from our local date, re-download!
                    // This could occur if the local file was modified and is now newer than the original.
                    if (urlConnection.getLastModified() != 0 && urlConnection.getLastModified() != urlConnection.getIfModifiedSince()) {
                        urlConnection.disconnect();
                        urlConnection = (HttpURLConnection) url.openConnection();
                    } else {
                        return false;
                    }
                }
            }

            if (urlConnection.getResponseCode() != 200) {
                throw new IOException("Failed to download " + url + ", HTTP-Status: "
                        + urlConnection.getResponseCode());
            }

            // Resolve a relative path to get a proper parent directory
            if (target.getParent() == null) {
                target = target.toAbsolutePath();
            }

            // Always download to a temp-file to avoid partially downloaded files persisting a VM crash/shutdown
            Files.createDirectories(target.getParent());
            Path tempFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".download");

            try {
                try (InputStream stream = urlConnection.getInputStream()) {
                    Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                try {
                    Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    // Atomic moves within the same directory should have worked.
                    // We fall back to the inferior normal move. We should log this issue, but there is no logger here.
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                }

                if (urlConnection.getLastModified() != 0) {
                    Files.setLastModifiedTime(target, FileTime.fromMillis(urlConnection.getLastModified()));
                }

                return true;
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static File getMCDir() {
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
