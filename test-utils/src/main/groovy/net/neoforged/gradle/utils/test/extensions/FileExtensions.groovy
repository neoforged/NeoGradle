package net.neoforged.gradle.utils.test.extensions

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
}
