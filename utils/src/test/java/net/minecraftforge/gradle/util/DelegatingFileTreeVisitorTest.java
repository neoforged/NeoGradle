package net.minecraftforge.gradle.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DelegatingFileTreeVisitorTest {

    @Test
    public void visitingADirectoryInvokesDelegate() {
        final FileVisitor delegate = mock(FileVisitor.class);
        final FileVisitDetails dirVisitDetails = mock(FileVisitDetails.class);
        final DelegatingFileTreeVisitor visitor = new DelegatingFileTreeVisitor(delegate);
        visitor.visitDir(dirVisitDetails);
        verify(delegate, times(1)).visitDir(dirVisitDetails);
    }

    @Test
    public void visitingAFileInvokesDelegate() {
        final FileVisitor delegate = mock(FileVisitor.class);
        final FileVisitDetails fileVisitDetails = mock(FileVisitDetails.class);
        final DelegatingFileTreeVisitor visitor = new DelegatingFileTreeVisitor(delegate);
        visitor.visitFile(fileVisitDetails);
        verify(delegate, times(1)).visitFile(fileVisitDetails);
    }
}