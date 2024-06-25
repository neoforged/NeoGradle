/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.platform.runtime.runtime.tasks;

import io.codechicken.diffpatch.cli.CliOperation;
import io.codechicken.diffpatch.cli.DiffOperation;
import io.codechicken.diffpatch.util.Input.MultiInput;
import io.codechicken.diffpatch.util.Output.MultiOutput;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.nio.file.Path;

public abstract class GenerateSourcePatches extends DefaultRuntime implements WithOutput, WithWorkspace {
    
    public GenerateSourcePatches() {
        getOriginalPrefix().convention("a/");
        getModifiedPrefix().convention("b/");
        getLineEnding().convention("\n");
        getShouldOutputVerboseLogging().convention(false);
        getContextLines().convention(-1);
        getShouldCreateAutomaticHeader().convention(true);
        getShouldPrintSummary().convention(false);
    }

    @TaskAction
    public void doTask() throws Exception {
        Path base = getBase().get().getAsFile().toPath();
        Path modified = getModified().get().getAsFile().toPath();
        Path output = getOutput().get().getAsFile().toPath();
        getLogger().info("Base: {}", base);
        getLogger().info("Modified: {}", modified);

        DiffOperation.Builder builder = DiffOperation.builder()
                .logTo(getLogger()::lifecycle)
                .baseInput(MultiInput.detectedArchive(base))
                .changedInput(MultiInput.detectedArchive(modified))
                .patchesOutput(MultiOutput.detectedArchive(output))
                .autoHeader(getShouldCreateAutomaticHeader().get())
                .level(getShouldOutputVerboseLogging().get() ? io.codechicken.diffpatch.util.LogLevel.ALL : io.codechicken.diffpatch.util.LogLevel.WARN)
                .summary(getShouldPrintSummary().get())
                .aPrefix(getOriginalPrefix().get())
                .bPrefix(getModifiedPrefix().get())
                .lineEnding(getLineEnding().get());

        if (getContextLines().get() != -1) {
            builder.context(getContextLines().get());
        }

        CliOperation.Result<DiffOperation.DiffSummary> result = builder.build().operate();

        int exit = result.exit;
        if (exit != 0 && exit != 1) {
            throw new RuntimeException("DiffPatch failed with exit code: " + exit);
        }
    }

    @InputFile
    public abstract RegularFileProperty getBase();

    @InputFile
    public abstract RegularFileProperty getModified();

    @Input
    @Optional
    public abstract Property<String> getOriginalPrefix();

    @Input
    @Optional
    public abstract Property<String> getModifiedPrefix();

    @Input
    public abstract Property<String> getLineEnding();

    @Input
    public abstract Property<Boolean> getShouldCreateAutomaticHeader();
    
    @Input
    @Optional
    public abstract Property<Integer> getContextLines();
    
    @Input
    @Optional
    public abstract Property<Boolean> getShouldOutputVerboseLogging();
    
    @Input
    @Optional
    public abstract Property<Boolean> getShouldPrintSummary();
}
