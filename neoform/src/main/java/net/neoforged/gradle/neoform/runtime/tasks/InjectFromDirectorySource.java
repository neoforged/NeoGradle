package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
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

    /**
     * The object factory that can be used to manage the internal subsystems of a gradle model.
     * Allows for the creation of for example file collections, trees and other components.
     *
     * @return The object factory.
     */
    @Inject
    public abstract ObjectFactory getObjectFactory();

    @Override
    public byte @Nullable [] tryReadFile(String path) throws IOException {
        File packageInfoTemplateFile = getDirectory().file("package-info-template.java").get().getAsFile();
        if (packageInfoTemplateFile.exists()) {
            return Files.readAllBytes(packageInfoTemplateFile.toPath());
        }
        return null;
    }

    @Override
    public void copyTo(ZipOutputStream out) throws IOException {
        final ConfigurableFileTree outputTree = getObjectFactory().fileTree();
        FileTree source = outputTree.from(getDirectory()).matching(createFilter());
        source.visit(new ZipBuildingFileTreeVisitor(out));
    }
}
