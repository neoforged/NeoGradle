package net.neoforged.gradle.neoform.runtime.tasks;

import org.gradle.api.file.FilePermissions;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RelativePath;

import java.io.File;
import java.io.FilePermission;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

/**
 * Fake implementation of {@link org.gradle.api.file.FileTreeElement} to allow
 * a {@link java.util.zip.ZipEntry} to be matched by a {@link org.gradle.api.specs.Spec}.
 */
public class ZipEntryFileTreeElement implements FileTreeElement {
    private final ZipEntry entry;

    public ZipEntryFileTreeElement(ZipEntry entry) {
        this.entry = entry;
    }

    @Override
    public File getFile() {
        return new File(entry.getName());
    }

    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public long getLastModified() {
        return entry.getLastModifiedTime().toMillis();
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public InputStream open() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyTo(OutputStream output) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean copyTo(File target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return getFile().getName();
    }

    @Override
    public String getPath() {
        return entry.getName().replace('\\', '/');
    }

    @Override
    public RelativePath getRelativePath() {
        return RelativePath.parse(!isDirectory(), getPath());
    }

    @Override
    public int getMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilePermissions getPermissions() {
        throw new UnsupportedOperationException();
    }
}
