package net.minecraftforge.gradle.util;

import com.google.common.io.ByteStreams;
import net.minecraftforge.gradle.base.file.FileTestingUtils;
import net.minecraftforge.gradle.base.file.TestFileTarget;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CopyingFileTreeVisitorTest {

    @Test
    public void visitingADirectoryCreatesADirectoryInTheTarget() throws IOException {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("directory");
        final FileVisitDetails details = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(details.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("directory");
        when(details.isDirectory()).thenReturn(true);

        Files.createDirectory(target.getPath());

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.getPath());

        visitor.visitDir(details);

        final Path targetPath = target.getPath().resolve("directory");
        assertTrue(Files.exists(targetPath));
        assertTrue(Files.isDirectory(targetPath));
    }

    @Test
    public void visitingAFileCreatesAFileInTheTarget() throws IOException {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("directory");
        final TestFileTarget source = FileTestingUtils.newSimpleTestFileTarget("file.txt");
        final FileVisitDetails details = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(details.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("file.txt");
        when(details.isDirectory()).thenReturn(true);
        doAnswer(invocation -> {
            final File file = invocation.getArgument(0);
            Files.copy(source.getPath(), file.toPath());
            return null;
        }).when(details).copyTo(ArgumentMatchers.<File>any());
        doAnswer(invocation -> {
            final OutputStream outputStream = invocation.getArgument(0);
            final InputStream inputStream = Files.newInputStream(source.getPath());
            ByteStreams.copy(inputStream, outputStream);
            return null;
        }).when(details).copyTo(ArgumentMatchers.<OutputStream>any());

        Files.createFile(source.getPath());

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.getPath());

        visitor.visitFile(details);

        final Path targetPath = target.getPath().resolve("file.txt");
        assertTrue(Files.exists(targetPath));
        assertTrue(Files.isRegularFile(targetPath));
    }

    @Test
    public void visitingAFileCreatesAFileInTheTargetAndCopiesItsContent() throws IOException {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("directory");
        final TestFileTarget source = FileTestingUtils.newSimpleTestFileTarget("file.txt");
        final FileVisitDetails details = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(details.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("file.txt");
        when(details.isDirectory()).thenReturn(true);
        doAnswer(invocation -> {
            final File file = invocation.getArgument(0);
            Files.copy(source.getPath(), file.toPath());
            return null;
        }).when(details).copyTo(ArgumentMatchers.<File>any());
        doAnswer(invocation -> {
            final OutputStream outputStream = invocation.getArgument(0);
            final InputStream inputStream = Files.newInputStream(source.getPath());
            ByteStreams.copy(inputStream, outputStream);
            return null;
        }).when(details).copyTo(ArgumentMatchers.<OutputStream>any());

        final byte[] content = "Hello World!".getBytes();
        Files.createFile(source.getPath());
        Files.write(source.getPath(), content);

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.getPath());

        visitor.visitFile(details);

        final Path targetPath = target.getPath().resolve("file.txt");
        assertTrue(Files.exists(targetPath));
        assertTrue(Files.isRegularFile(targetPath));
        assertArrayEquals(content, Files.readAllBytes(targetPath));
    }

    @Test
    public void tryingToUseAFileAsATargetThrowsARuntimeException() throws IOException {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("file.txt");

        Files.createFile(target.getPath());

        assertThrows(RuntimeException.class, () -> new CopyingFileTreeVisitor(target.getPath()));
    }

    @Test
    public void passingANotExistingDirectoryCreatesIt() throws IOException  {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("directory");

        assertFalse(Files.exists(target.getPath()));

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.getPath());

        final Path targetPath = target.getPath();

        assertTrue(Files.exists(targetPath));
        assertTrue(Files.isDirectory(targetPath));
    }

    @Test
    public void passingInADirectoryWithContentDeletesTheContent() throws IOException {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("directory");
        final Path file = target.getPath().resolve("test.txt");

        Files.createDirectories(target.getPath());
        Files.createFile(file);

        assertTrue(Files.exists(file));

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.getPath());

        assertTrue(Files.exists(target.getPath()));
        assertTrue(Files.isDirectory(target.getPath()));
        assertFalse(Files.exists(file));
    }

    @Test
    public void passingInAFileUsesItsPath() throws IOException {
        final TestFileTarget target = FileTestingUtils.newSimpleTestFileTarget("file.txt");
        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.getFile());

        assertEquals(target.getPath(), visitor.getDirectory());
    }
}