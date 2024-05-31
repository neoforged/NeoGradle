package net.neoforged.gradle.utils.test.extensions

import groovy.transform.CompileStatic

/**
 * Extensions for the [File] class.
 */
@CompileStatic
class FileExtensions {

    /**
     * @return the absolute path of the file with backslashes escaped.
     */
    static String getPropertiesPath(final File self) {
        return self.absolutePath.replace("\\", "\\\\")
    }
}
