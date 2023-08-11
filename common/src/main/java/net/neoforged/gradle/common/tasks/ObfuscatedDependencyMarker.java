package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.util.AdaptingZipBuildingFileTreeVisitor;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class ObfuscatedDependencyMarker extends DefaultTask implements WithOutput, WithWorkspace {


    public ObfuscatedDependencyMarker() {
        super();
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir("obfuscated").flatMap(directory -> getObfuscatedJar().map(input -> directory.file(input.getAsFile().getName().substring(0, input.getAsFile().getName().length() - 4) + "-marked.jar"))));
    }

    @TaskAction
    public void doMark() {
        try {
            final File outputJar = this.ensureFileWorkspaceReady(getOutput());
            final FileOutputStream fileOutputStream = new FileOutputStream(outputJar);
            final ZipOutputStream outputZipStream = new ZipOutputStream(fileOutputStream);
            final AdaptingZipBuildingFileTreeVisitor zipBuildingFileTreeVisitor = new AdaptingZipBuildingFileTreeVisitor(outputZipStream, (fileVisitDetails, outputStream) -> {
                if (!fileVisitDetails.getRelativePath().getPathString().equals("META-INF/MANIFEST.MF")) {
                    fileVisitDetails.copyTo(outputStream);
                    return;
                }
                try {
                    Manifest manifest = new Manifest(fileVisitDetails.open());
                    Attributes mainAttributes = manifest.getMainAttributes();
                    mainAttributes.putValue("Obfuscated", "true");
                    mainAttributes.putValue("Obfuscated-By", "NeoGradle");

                    manifest.write(outputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write manifest", e);
                }
            });
            getProject().zipTree(getObfuscatedJar().get()).visit(zipBuildingFileTreeVisitor);
            outputZipStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create recompiled output jar", e);
        }
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getObfuscatedJar();
}
