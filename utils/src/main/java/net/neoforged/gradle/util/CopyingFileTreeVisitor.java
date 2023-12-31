package net.neoforged.gradle.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CopyingFileTreeVisitor implements FileVisitor {

    private final Path directory;
    private final boolean processDirectories;

    public CopyingFileTreeVisitor(Path directory, boolean processDirectories) {
        this.directory = directory;
        this.processDirectories = processDirectories;

        initTargetDirectory(directory);
    }

    public CopyingFileTreeVisitor(File directory, boolean processDirectories) {
        this(directory.toPath(), processDirectories);
    }

    public CopyingFileTreeVisitor(Path directory) {
        this(directory, true);
    }

    public CopyingFileTreeVisitor(File directory) {
        this(directory, true);
    }


    @VisibleForTesting
    static void initTargetDirectory(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                return;
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + directory, e);
            }
        }

        if (Files.isRegularFile(directory)) {
            throw new IllegalArgumentException("The given path is a file, not a directory: " + directory);
        }

        try {
            FileUtils.delete(directory);
        } catch (IOException e) {
            throw new RuntimeException("Cloud not clean up target directory: " + directory, e);
        }

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory: " + directory, e);
        }
    }

    @Override
    public void visitDir(FileVisitDetails dirDetails) {
        if (!processDirectories)
            return;

        final Path target = directory.resolve(dirDetails.getRelativePath().getPathString());
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory: " + target, e);
        }
    }

    @Override
    public void visitFile(FileVisitDetails fileDetails) {
        final Path target = directory.resolve(fileDetails.getRelativePath().getPathString());
        try {
            Files.createDirectories(target.getParent());
            fileDetails.copyTo(Files.newOutputStream(target));
        } catch (IOException e) {
            throw new RuntimeException("Could not create file: " + target, e);
        }
    }

    @VisibleForTesting
    Path getDirectory() {
        return directory;
    }
}
