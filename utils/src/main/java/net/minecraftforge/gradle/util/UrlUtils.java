package net.minecraftforge.gradle.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for working with URLs
 */
public final class UrlUtils {

    private UrlUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: UrlUtils. This is a utility class");
    }

    /**
     * Gets the size of a file at the given URL
     * Sends of a HEAD request to the URL and returns the Content-Length header
     *
     * @param url The URL to get the size of
     * @return The size of the file at the given URL
     */
    public static long getFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getContentLengthLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
