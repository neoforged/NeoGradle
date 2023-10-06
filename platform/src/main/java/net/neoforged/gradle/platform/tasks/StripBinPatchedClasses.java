package net.neoforged.gradle.platform.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.util.FilteringZipBuildingFileTreeVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class StripBinPatchedClasses extends DefaultRuntime implements WithOutput, WithWorkspace {
    
    @TaskAction
    public void doTask() throws Exception {
        final Set<String> cleanedFiles = new HashSet<>();
        final Set<String> cleanedDirectories = new HashSet<>();
        getArchiveOperations().zipTree(getClean().get().getAsFile()).visit(new FileVisitor() {
            @Override
            public void visitDir(@NotNull FileVisitDetails dirDetails) {
                cleanedDirectories.add(dirDetails.getRelativePath().getPathString());
            }
            
            @Override
            public void visitFile(@NotNull FileVisitDetails fileDetails) {
                cleanedFiles.add(fileDetails.getRelativePath().getPathString());
            }
        });
        
        
        final File output = ensureFileWorkspaceReady(getOutput());
        try (OutputStream outputStream = new FileOutputStream(output);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            final FilteringZipBuildingFileTreeVisitor visitor = new FilteringZipBuildingFileTreeVisitor(
                    zipOutputStream,
                    details -> !cleanedDirectories.contains(details.getRelativePath().getPathString()),
                    details -> !cleanedFiles.contains(details.getRelativePath().getPathString())
            );
            getArchiveOperations().zipTree(getCompiled().get().getAsFile()).getAsFileTree().visit(visitor);
        }
    }
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getClean();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getCompiled();
}
