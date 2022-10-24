package net.minecraftforge.gradle.mcp.util;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public final class ZipUtils {

    private ZipUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ZipUtils. This is a utility class");
    }

    public static int getZipEntryCount(File zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            return zip.size();
        }
    }
}
