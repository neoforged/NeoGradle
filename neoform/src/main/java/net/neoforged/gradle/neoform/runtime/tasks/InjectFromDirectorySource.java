package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipOutputStream;

/**
 * Inject content from a directory on disk.
 * @see InjectZipContent
 */
public abstract class InjectFromDirectorySource extends AbstractInjectSource {
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    public abstract DirectoryProperty getDirectory();

    @Override
    public byte @Nullable [] tryReadFile(String path) throws IOException {
        File packageInfoTemplateFile = getDirectory().file("package-info-template.java").get().getAsFile();
        if (packageInfoTemplateFile.exists()) {
            return Files.readAllBytes(packageInfoTemplateFile.toPath());
        }
        return null;
    }

    @Override
    public void copyTo(ZipOutputStream out) {
        FileTree source = getDirectory().getAsFileTree().matching(createFilter());
        source.visit(new ZipBuildingFileTreeVisitor(out));
    }
}
