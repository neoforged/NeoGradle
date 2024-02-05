/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.platform.runtime.runtime.tasks;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import codechicken.diffpatch.util.LoggingOutputStream;
import codechicken.diffpatch.util.PatchMode;
import codechicken.diffpatch.util.archiver.ArchiveFormat;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.file.Path;

@CacheableTask
public abstract class ApplyPatches extends DefaultRuntime implements WithWorkspace, WithOutput {
   
   public ApplyPatches() {
      getPatchesPrefix().convention("");
      getOriginalPrefix().convention("a/");
      getModifiedPrefix().convention("b/");
      getShouldFailOnPatchFailure().convention(false);
      getPatchMode().convention(getShouldFailOnPatchFailure().map(fail -> fail ? PatchMode.ACCESS : PatchMode.FUZZY));
      getMinimalFuzzingQuality().convention(0.90f); // The 0.5 default in DiffPatch is too low.
   }
   
   @TaskAction
   public void doTask() throws Exception {
      if (!getPatches().isPresent()) {
         FileUtils.copyFile(getBase().getAsFile().get(), getOutput().get().getAsFile());
         return;
      }
      
      Path outputPath = ensureFileWorkspaceReady(getOutput()).toPath();

      final Directory rejectsDir = getRejects().get();
      Path rejectsPath = rejectsDir.getAsFile().toPath();
      
      PatchOperation.Builder builder = PatchOperation.builder()
                                             .logTo(new LoggingOutputStream(getLogger(), LogLevel.LIFECYCLE))
                                             .basePath(getBase().get().getAsFile().toPath())
                                             .patchesPath(getPatches().get().getAsFile().toPath())
                                             .outputPath(outputPath, ArchiveFormat.findFormat(outputPath.getFileName()))
                                             .rejectsPath(rejectsPath, ArchiveFormat.findFormat(rejectsPath.getFileName()))
                                             .mode(getPatchMode().get())
                                             .aPrefix(getOriginalPrefix().get())
                                             .bPrefix(getModifiedPrefix().get())
                                             .level(getShouldFailOnPatchFailure().get() ? codechicken.diffpatch.util.LogLevel.WARN : codechicken.diffpatch.util.LogLevel.ALL)
                                             .patchesPrefix(getPatchesPrefix().get());

      builder.minFuzz(getMinimalFuzzingQuality().get());

      if (getMaximalFuzzingOffset().isPresent()) {
         builder.maxOffset(getMaximalFuzzingOffset().get());
      }
      
      CliOperation.Result<PatchOperation.PatchesSummary> result = builder.build().operate();
      
      int exit = result.exit;
      if (exit != 0 && exit != 1) {
         throw new RuntimeException("DiffPatch failed with exit code: " + exit);
      }
      if (exit != 0 && getShouldFailOnPatchFailure().get()) {
         throw new RuntimeException("Patches failed to apply.");
      }
   }
   
   // TODO: split into separate (exclusive) properties for directory or file?
   @InputFile
   @PathSensitive(PathSensitivity.NONE)
   public abstract RegularFileProperty getBase();
   
   @InputDirectory
   @PathSensitive(PathSensitivity.NONE)
   public abstract DirectoryProperty getPatches();
   
   @OutputDirectory
   @Optional
   public abstract DirectoryProperty getRejects();
   
   @Input
   @Optional
   public abstract Property<ArchiveFormat> getRejectsFormat();
   
   @Input
   @Optional
   public abstract Property<PatchMode> getPatchMode();
   
   @Input
   @Optional
   public abstract Property<String> getPatchesPrefix();
   
   @Input
   @Optional
   public abstract Property<String> getOriginalPrefix();
   
   @Input
   @Optional
   public abstract Property<String> getModifiedPrefix();
   
   @Input
   @Optional
   public abstract Property<Float> getMinimalFuzzingQuality();
   
   @Input
   @Optional
   public abstract Property<Integer> getMaximalFuzzingOffset();
   
   @Input
   @Optional
   public abstract Property<Boolean> getShouldFailOnPatchFailure();
}
