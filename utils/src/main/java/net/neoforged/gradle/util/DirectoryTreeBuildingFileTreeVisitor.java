package net.neoforged.gradle.util;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryTreeBuildingFileTreeVisitor implements FileVisitor
{
    private final Path root;

    public DirectoryTreeBuildingFileTreeVisitor(final Path root) {
        this.root = root;
        CopyingFileTreeVisitor.initTargetDirectory(root);
    }

    @Override
    public void visitDir(final FileVisitDetails dirDetails)
    {
        final Path target = root.resolve(dirDetails.getPath());
        try
        {
            Files.createDirectories(target);
        }
        catch (IOException e)
        {
            throw new GradleException("Failed to create the directory tree element: " + dirDetails.getPath() + " under: " + root.toAbsolutePath(), e);
        }
    }

    @Override
    public void visitFile(final FileVisitDetails fileDetails)
    {
        final Path target = root.resolve(fileDetails.getPath()).getParent();
        try
        {
            Files.createDirectories(target);
        }
        catch (IOException e)
        {
            throw new GradleException("Failed to create the directory tree element: " + fileDetails.getPath() + " under: " + root.toAbsolutePath(), e);
        }
    }
}
