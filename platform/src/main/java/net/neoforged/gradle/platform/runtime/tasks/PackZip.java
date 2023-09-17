package net.neoforged.gradle.platform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class PackZip extends DefaultRuntime {
   
   @TaskAction
   public void doRun() throws Exception {
      final File output = ensureFileWorkspaceReady(getOutput());
      try (OutputStream outputStream = new FileOutputStream(output);
           ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
         final ZipBuildingFileTreeVisitor visitor = new ZipBuildingFileTreeVisitor(zipOutputStream);
         getInputFiles().getAsFileTree().visit(visitor);
      }
   }
   
   @InputFiles
   public abstract ConfigurableFileCollection getInputFiles();
}
