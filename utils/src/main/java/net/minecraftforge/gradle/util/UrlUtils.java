package net.minecraftforge.gradle.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UrlUtils {

    private UrlUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: UrlUtils. This is a utility class");
    }

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
