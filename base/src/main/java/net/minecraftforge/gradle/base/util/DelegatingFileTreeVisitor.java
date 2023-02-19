package net.minecraftforge.gradle.base.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

public class DelegatingFileTreeVisitor implements FileVisitor {

    protected final FileVisitor delegate;

    public DelegatingFileTreeVisitor(FileVisitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void visitDir(FileVisitDetails dirDetails) {
        delegate.visitDir(dirDetails);
    }

    @Override
    public void visitFile(FileVisitDetails fileDetails) {
        delegate.visitFile(fileDetails);
    }
}
