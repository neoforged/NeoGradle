package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipOutputStream;

/**
 * Inject content from a directory on disk.
 * @see InjectZipContent
 */
public abstract class InjectFromFileTreeSource extends AbstractInjectSource {

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getFiles();

    @Override
    public byte @Nullable [] tryReadFile(String path) throws IOException {
        final FileTree matching = getFiles().getAsFileTree().matching(createFilter())
                .matching(pattern -> pattern.include(path));
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
        source.visit(new ZipBuildingFileTreeVisitor(out));
    }
}
