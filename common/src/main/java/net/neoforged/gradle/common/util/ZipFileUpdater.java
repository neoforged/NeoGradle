package net.neoforged.gradle.common.util;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class ZipFileUpdater {

    public static void addFileToZip(File zipFile, File fileToAdd, String entryName) throws IOException {
        // Temporary zip file
        File tempFile = File.createTempFile(zipFile.getName(), null);
        tempFile.delete();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {

            // Copy existing entries to the new zip file
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(entry.getName()));
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                zis.closeEntry();
            }

            // Add the new file to the zip file
            try (InputStream fis = new FileInputStream(fileToAdd)) {
                zos.putNextEntry(new ZipEntry(entryName));
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            }
        }

        // Replace the old zip file with the new zip file
        Files.delete(zipFile.toPath());
        Files.move(tempFile.toPath(), zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}