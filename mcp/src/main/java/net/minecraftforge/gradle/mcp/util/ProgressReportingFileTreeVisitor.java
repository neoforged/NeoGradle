package net.minecraftforge.gradle.mcp.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

import java.util.function.Consumer;

public class ProgressReportingFileTreeVisitor implements FileVisitor {

    private final FileVisitor delegate;
    private final Consumer<String> onDirectoryProgress;
    private final Consumer<String> onFileProgress;

    public ProgressReportingFileTreeVisitor(FileVisitor delegate, Consumer<String> onDirectoryProgress, Consumer<String> onFileProgress) {
        this.delegate = delegate;
        this.onDirectoryProgress = onDirectoryProgress;
        this.onFileProgress = onFileProgress;
    }

    @Override
    public void visitDir(FileVisitDetails fileVisitDetails) {
        onDirectoryProgress.accept(fileVisitDetails.getRelativePath().getPathString());
        delegate.visitDir(fileVisitDetails);
    }

    @Override
    public void visitFile(FileVisitDetails fileVisitDetails) {
        onFileProgress.accept(fileVisitDetails.getRelativePath().getPathString());
        delegate.visitFile(fileVisitDetails);
    }
}
