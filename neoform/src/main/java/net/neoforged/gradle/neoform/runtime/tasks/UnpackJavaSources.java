package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@CacheableTask
public abstract class UnpackJavaSources extends DefaultRuntime {

    public UnpackJavaSources() {
        getUnpackingTarget().convention(getOutputDirectory().map(dir -> dir.dir("unpacked")));
    }

    @TaskAction
    public void doTask() throws IOException {
        final Path output = getUnpackingTarget().getAsFile().get().toPath();
        final Set<Path> existingFiles = new HashSet<>();
        if (!Files.exists(output)) {
            Files.createDirectories(output);
        } else {
            if (Files.isRegularFile(output)) {
                throw new IOException("Output file " + output + " is a regular file!");
            }
            Files.walkFileTree(output, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    existingFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        final File input = getInputZip().getAsFile().get();

        final FileTree source = getProject().zipTree(input);
        source.visit(fileVisitDetails -> {
            if (fileVisitDetails.isDirectory() || !fileVisitDetails.getName().endsWith(".java")) return;
            try (final InputStream is = fileVisitDetails.open()) {
                final Path relativePath = output.resolve(fileVisitDetails.getRelativePath().getPathString());
                existingFiles.remove(relativePath);

                try {
                    final byte[] existing = Files.readAllBytes(relativePath);
                    final byte[] toWrite = IOUtils.toByteArray(is);
                    if (Arrays.equals(existing, toWrite)) return; // Do not write the file again if it's the same

                    Files.write(relativePath, toWrite);
                    return;
                } catch (Exception ignored) {}

                FileUtils.copyToFile(is, relativePath.toFile());
            } catch (IOException ex) {
                throw new RuntimeException("Failed to copy file " + fileVisitDetails.getName(), ex);
            }
        });

        for (Path file : existingFiles) {
            Files.delete(file);
        }
    }

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputZip();

    @OutputDirectory
    public abstract DirectoryProperty getUnpackingTarget();
}
