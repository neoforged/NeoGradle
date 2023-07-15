package net.neoforged.gradle.util;

import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdaptingZipBuildingFileTreeVisitorTest extends ZipBuildingFileTreeVisitorTest {

    @Override
    protected @NotNull ZipBuildingFileTreeVisitor createVisitor(ZipOutputStream target) {
        return new AdaptingZipBuildingFileTreeVisitor(target, FileTreeElement::copyTo);
    }

    @Test
    public void visitingAFileInvokesAdapterWhichCanChooseToNotCopyTheFileContentsButStillCreatesTheFile() throws IOException {
        final ZipOutputStream target = mock(ZipOutputStream.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final RelativePath relativePath = mock(RelativePath.class);

        when(fileVisitDetails.getRelativePath()).thenReturn(relativePath);
        when(relativePath.getPathString()).thenReturn("some/path");

        final AdaptingZipBuildingFileTreeVisitor visitor = new AdaptingZipBuildingFileTreeVisitor(target, (file, stream) -> {});
        visitor.visitFile(fileVisitDetails);

        verify(target, times(1)).putNextEntry(any());
        verify(fileVisitDetails, times(0)).copyTo(ArgumentMatchers.<OutputStream>any());
        verify(target, times(1)).closeEntry();
    }
}