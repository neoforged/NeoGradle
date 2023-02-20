package net.minecraftforge.gradle.vanilla.runtime.tasks;

import net.minecraftforge.gradle.util.DelegatingFileTreeVisitor;
import net.minecraftforge.gradle.util.ZipBuildingFileTreeVisitor;
import net.minecraftforge.gradle.common.runtime.tasks.DefaultRuntime;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class CleanArtifact extends DefaultRuntime {

    @TaskAction
    public void doClean() {
        try {
            final File outputJar = this.ensureFileWorkspaceReady(getOutput());
            final FileOutputStream fileOutputStream = new FileOutputStream(outputJar);
            final ZipOutputStream outputZipStream = new ZipOutputStream(fileOutputStream);

            final DelegatingFileTreeVisitor visitor = new DelegatingFileTreeVisitor(new ZipBuildingFileTreeVisitor(outputZipStream)) {

                @Override
                public void visitDir(FileVisitDetails fileVisitDetails) {
                    if (!fileVisitDetails.getRelativePath().getPathString().startsWith("assets/") &&
                            !fileVisitDetails.getRelativePath().getPathString().startsWith("data/") &&
                            !fileVisitDetails.getRelativePath().getPathString().startsWith("net/") &&
                            !fileVisitDetails.getRelativePath().getPathString().startsWith("META-INF/") &&
                            !fileVisitDetails.getRelativePath().getPathString().contains("mojang") &&
                            !fileVisitDetails.getRelativePath().getPathString().contains("minecraft")) {
                        return;
                    }

                    super.visitDir(fileVisitDetails);
                }

                @Override
                public void visitFile(FileVisitDetails fileVisitDetails) {
                    if (fileVisitDetails.getRelativePath().getPathString().equals("META-INF/MANIFEST.MF")) {
                        return;
                    }

                    if (fileVisitDetails.getRelativePath().getPathString().contains("/")) {
                        //Is in directory. Only leave:
                        if (!fileVisitDetails.getRelativePath().getPathString().startsWith("assets/") &&
                                !fileVisitDetails.getRelativePath().getPathString().startsWith("data/") &&
                                !fileVisitDetails.getRelativePath().getPathString().startsWith("net/") &&
                                !fileVisitDetails.getRelativePath().getPathString().startsWith("META-INF/") &&
                                !fileVisitDetails.getRelativePath().getPathString().contains("mojang") &&
                                !fileVisitDetails.getRelativePath().getPathString().contains("minecraft")) {
                            return;
                        }
                    }

                    super.visitFile(fileVisitDetails);
                }
            };
            getProject().zipTree(getInput().get()).visit(visitor);
            outputZipStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cleaned output jar", e);
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();
}
