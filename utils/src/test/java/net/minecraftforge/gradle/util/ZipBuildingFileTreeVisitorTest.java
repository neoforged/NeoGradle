package net.minecraftforge.gradle.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZipBuildingFileTreeVisitorTest {

    @Test
    public void visitingADirectoryCreatesAnEntryInZip() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        visitor.visitDir(fileVisitDetails);

        verify(target, times(1)).putNextEntry(any());
        verify(target, times(1)).closeEntry();
    }

    @Test
    public void throwingAZipExceptionForDuplicateDirectoriesDoesNotThrowAnException() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("duplicate entry: some/path/"); }).
                when(target).putNextEntry(any());

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertDoesNotThrow(() -> visitor.visitDir(fileVisitDetails));
    }

    @Test
    public void throwingAZipExceptionForAnythingOtherThenDuplicateDirectoriesThrowsAnException() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("some other exception"); }).
                when(target).putNextEntry(any());

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertThrows(RuntimeException.class, () -> visitor.visitDir(fileVisitDetails));
    }

    @Test
    public void throwingAnIOExceptionDuringEntryPuttingWhileVisitingADirectoryThrowsAnException() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("some other exception"); }).
                when(target).putNextEntry(any());

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertThrows(RuntimeException.class, () -> visitor.visitDir(fileVisitDetails));
    }

    @Test
    public void throwingAnIOExceptionDuringEntryClosingWhileVisitingADirectoryThrowsAnException() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("some other exception"); }).
                when(target).closeEntry();

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertThrows(RuntimeException.class, () -> visitor.visitDir(fileVisitDetails));
    }

    @Test
    public void visitingAFileCreatesAnEntryInZipAndCopiesItUsingAStream() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");
        when(relativePath.getPathString()).thenReturn("some/path");

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        visitor.visitFile(fileVisitDetails);

        verify(target, times(1)).putNextEntry(any());
        verify(fileVisitDetails, times(1)).copyTo(ArgumentMatchers.<OutputStream>any());
        verify(target, times(1)).closeEntry();
    }

    @Test
    public void throwingAnIOExceptionDuringEntryPuttingWhileVisitingAFileThrowsAnException() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("some other exception"); }).
                when(target).putNextEntry(any());

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertThrows(RuntimeException.class, () -> visitor.visitFile(fileVisitDetails));
    }

    @Test
    public void throwingAnIOExceptionDuringCopyingWhileVisitingAFileThrowsAnException() {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("some other exception"); }).
                when(fileVisitDetails).copyTo(ArgumentMatchers.<OutputStream>any());

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertThrows(RuntimeException.class, () -> visitor.visitFile(fileVisitDetails));
    }

    @Test
    public void throwingAnIOExceptionDuringEntryClosingWhileVisitingAFileThrowsAnException() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        doAnswer((a) -> { throw new IOException("some other exception"); }).
                when(target).closeEntry();

        final ZipBuildingFileTreeVisitor visitor = createVisitor(target);
        assertThrows(RuntimeException.class, () -> visitor.visitFile(fileVisitDetails));
    }

    @NotNull
    protected ZipBuildingFileTreeVisitor createVisitor(ZipOutputStream target) {
        return new ZipBuildingFileTreeVisitor(target);
    }
}