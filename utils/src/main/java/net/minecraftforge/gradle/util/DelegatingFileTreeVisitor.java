package net.minecraftforge.gradle.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

/**
 * A {@link FileVisitor} that delegates all calls to another {@link FileVisitor}.
 * Create an anonymous class that extends this class and override the methods you want to intercept.
 * <p>
 * The class is very useful when you want to intercept some file or directory visits and delegate the rest to another visitor.
 */
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
