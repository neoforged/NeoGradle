package net.minecraftforge.gradle.base.file;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFileTarget {

    private final FileSystem fileSystem;
    private final Path path;
    private final File file;

    public TestFileTarget(FileSystem fileSystem, Path path) {
        this.fileSystem = fileSystem;
        this.path = path;

        this.file = mock(File.class);
        when(file.toPath()).thenReturn(path);
        when(file.getName()).thenReturn(path.getFileName().toString());

        when(file.isFile()).thenAnswer(invocation -> Files.isRegularFile(path));
        when(file.isDirectory()).thenAnswer(invocation -> Files.isDirectory(path));
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public Path getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }
}
