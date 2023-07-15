package net.neoforged.gradle.util;

import com.google.common.io.ByteStreams;
import net.minecraftforge.trainingwheels.base.file.FileTestingUtils;
import net.minecraftforge.trainingwheels.base.file.PathFile;
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
        final PathFile target = FileTestingUtils.newSimpleTestFile("directory");
        final FileVisitDetails details = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(details.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("directory");
        when(details.isDirectory()).thenReturn(true);

        Files.createDirectory(target.toPath());

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.toPath());

        visitor.visitDir(details);

        final Path tartoPath = target.toPath().resolve("directory");
        assertTrue(Files.exists(tartoPath));
        assertTrue(Files.isDirectory(tartoPath));
    }

    @Test
    public void visitingAFileCreatesAFileInTheTarget() throws IOException {
        final PathFile target = FileTestingUtils.newSimpleTestFile("directory");
        final PathFile source = FileTestingUtils.newSimpleTestFile("file.txt");
        final FileVisitDetails details = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(details.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("file.txt");
        when(details.isDirectory()).thenReturn(true);
        doAnswer(invocation -> {
            final File file = invocation.getArgument(0);
            Files.copy(source.toPath(), file.toPath());
            return null;
        }).when(details).copyTo(ArgumentMatchers.<File>any());
        doAnswer(invocation -> {
            final OutputStream outputStream = invocation.getArgument(0);
            final InputStream inputStream = Files.newInputStream(source.toPath());
            ByteStreams.copy(inputStream, outputStream);
            return null;
        }).when(details).copyTo(ArgumentMatchers.<OutputStream>any());

        Files.createFile(source.toPath());

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.toPath());

        visitor.visitFile(details);

        final Path tartoPath = target.toPath().resolve("file.txt");
        assertTrue(Files.exists(tartoPath));
        assertTrue(Files.isRegularFile(tartoPath));
    }

    @Test
    public void visitingAFileCreatesAFileInTheTargetAndCopiesItsContent() throws IOException {
        final PathFile target = FileTestingUtils.newSimpleTestFile("directory");
        final PathFile source = FileTestingUtils.newSimpleTestFile("file.txt");
        final FileVisitDetails details = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(details.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("file.txt");
        when(details.isDirectory()).thenReturn(true);
        doAnswer(invocation -> {
            final File file = invocation.getArgument(0);
            Files.copy(source.toPath(), file.toPath());
            return null;
        }).when(details).copyTo(ArgumentMatchers.<File>any());
        doAnswer(invocation -> {
            final OutputStream outputStream = invocation.getArgument(0);
            final InputStream inputStream = Files.newInputStream(source.toPath());
            ByteStreams.copy(inputStream, outputStream);
            return null;
        }).when(details).copyTo(ArgumentMatchers.<OutputStream>any());

        final byte[] content = "Hello World!".getBytes();
        Files.createFile(source.toPath());
        Files.write(source.toPath(), content);

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.toPath());

        visitor.visitFile(details);

        final Path tartoPath = target.toPath().resolve("file.txt");
        assertTrue(Files.exists(tartoPath));
        assertTrue(Files.isRegularFile(tartoPath));
        assertArrayEquals(content, Files.readAllBytes(tartoPath));
    }

    @Test
    public void tryingToUseAFileAsATargetThrowsARuntimeException() throws IOException {
        final PathFile target = FileTestingUtils.newSimpleTestFile("file.txt");

        Files.createFile(target.toPath());

        assertThrows(RuntimeException.class, () -> new CopyingFileTreeVisitor(target.toPath()));
    }

    @Test
    public void passingANotExistingDirectoryCreatesIt() throws IOException  {
        final PathFile target = FileTestingUtils.newSimpleTestFile("directory");

        assertFalse(Files.exists(target.toPath()));

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.toPath());

        final Path tartoPath = target.toPath();

        assertTrue(Files.exists(tartoPath));
        assertTrue(Files.isDirectory(tartoPath));
    }

    @Test
    public void passingInADirectoryWithContentDeletesTheContent() throws IOException {
        final PathFile target = FileTestingUtils.newSimpleTestFile("directory");
        final Path file = target.toPath().resolve("test.txt");

        Files.createDirectories(target.toPath());
        Files.createFile(file);

        assertTrue(Files.exists(file));

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target.toPath());

        assertTrue(Files.exists(target.toPath()));
        assertTrue(Files.isDirectory(target.toPath()));
        assertFalse(Files.exists(file));
    }

    @Test
    public void passingInAFileUsesItsPath() throws IOException {
        final PathFile target = FileTestingUtils.newSimpleTestFile("file.txt");
        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(target);

        assertEquals(target.toPath(), visitor.getDirectory());
    }
}