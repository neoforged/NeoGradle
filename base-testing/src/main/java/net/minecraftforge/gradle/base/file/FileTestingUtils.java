package net.minecraftforge.gradle.base.file;

import com.google.common.jimfs.Jimfs;

import java.nio.file.FileSystem;

/**
 * Utility class for creating {@link TestFileTarget} instances, with particular properties.
 */
public final class FileTestingUtils {

    private FileTestingUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileTestingUtils. This is a utility class");
    }

    /**
     * Creates a new {@link TestFileTarget} instance, with a {@link FileSystem} that is backed
     * in memory. The {@link FileSystem} is created using {@link Jimfs#newFileSystem()}.
     *
     * @param path The path to the file, relative to the root of the file system.
     * @return The new {@link TestFileTarget} instance.
     */
    public static TestFileTarget newSimpleTestFileTarget(String path) {
        final FileSystem fileSystem = Jimfs.newFileSystem();
        return new TestFileTarget(fileSystem, fileSystem.getPath(path));
    }
}
