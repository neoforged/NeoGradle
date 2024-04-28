package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Inject content from a directory on disk.
 * @see InjectZipContent
 */
public abstract class InjectFromFileTreeSource extends AbstractInjectSource {

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getFiles();

    @Optional
    @Input
    public abstract Property<String> getTreePrefix();

    @Override
    public byte @Nullable [] tryReadFile(String path) throws IOException {
        final String lookupPath = buildTreePrefix() + path;
        final FileTree matching = getFiles().getAsFileTree().matching(createFilter())
                .matching(pattern -> pattern.include(lookupPath));
        if (matching.isEmpty()) {
            return null;
        }
        return Files.readAllBytes(matching.getSingleFile().toPath());
    }

    @Override
    public void copyTo(ZipOutputStream out) throws IOException {
        if (getFiles().isEmpty()) {
            return;
        }

        final FileTree source = getFiles().getAsFileTree().matching(createFilter());
        source.visit(new ZipBuildingFileTreeVisitor(out) {
            @Override
            protected ZipEntry createDirectory(FileVisitDetails fileVisitDetails) {
                String path = fileVisitDetails.getRelativePath().getPathString();
                if (path.startsWith(buildTreePrefix())) {
                    path = path.substring(buildTreePrefix().length());
                }

                if (!path.endsWith("/")) {
                    path += "/";
                }

                return new ZipEntry(path);
            }

            @Override
            protected ZipEntry createFile(FileVisitDetails fileVisitDetails) {
                String path = fileVisitDetails.getRelativePath().getPathString();
                if (path.startsWith(buildTreePrefix())) {
                    path = path.substring(buildTreePrefix().length());
                }

                return new ZipEntry(path);
            }
        });
    }

    private String buildTreePrefix() {
        if (getTreePrefix().isPresent()) {
            final String prefix = getTreePrefix().get();
            if (!prefix.endsWith("/")) {
                return prefix + "/";
            }
            return prefix;
        }

        return "";
    }
}
