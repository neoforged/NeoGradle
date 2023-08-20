package net.neoforged.gradle.util;

import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

/**
 * A serializable wrapper for a file.
 * Useful for passing files to tasks that are not serializable.
 */
public final class FileWrapper implements Serializable {
    private static final long serialVersionUID = 5391682724482743076L;
    private final File file;

    public FileWrapper(File file) {
        this.file = file;
    }

    /**
     * Returns the file that this wrapper internally holds.
     *
     * @return The file.
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public File getFile() {
        return this.file;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final FileWrapper that = (FileWrapper) obj;
        return Objects.equals(this.file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }

    @Override
    public String toString() {
        return "FileWrapper[" +
                "file=" + file + ']';
    }

}
