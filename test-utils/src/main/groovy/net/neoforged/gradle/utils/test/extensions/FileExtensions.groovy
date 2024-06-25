package net.neoforged.gradle.utils.test.extensions

import groovy.transform.CompileStatic

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Extensions for the [File] class.
 */
class FileExtensions {

    /**
     * @return the absolute path of the file with backslashes escaped.
     */
    static String getPropertiesPath(final File self) {
        return self.absolutePath.replace("\\", "\\\\")
    }

    static File getZipEntry(final File self, final String path) {
        final ZipFile zipFile = new ZipFile(self)
        final ZipEntry entry = zipFile.getEntry(path)
        if (entry == null) {
            throw new IllegalArgumentException("Entry not found: $path")
        }

        final File tempFile = File.createTempFile("zipEntry", ".tmp")
        final InputStream zipInputStream = zipFile.getInputStream(entry)
        final OutputStream tempOutputStream = new FileOutputStream(tempFile)
        tempOutputStream << zipInputStream
        tempOutputStream.close()
        zipInputStream.close()
        zipFile.close()

        return tempFile;
    }
}
