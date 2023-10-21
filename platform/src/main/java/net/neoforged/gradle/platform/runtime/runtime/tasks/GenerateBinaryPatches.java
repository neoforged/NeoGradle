/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.platform.runtime.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.common.util.Constants;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.stream.Collectors;

public abstract class GenerateBinaryPatches extends Execute implements WithOutput, WithWorkspace {
    public GenerateBinaryPatches() {
        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), Constants.BINPATCHER));
        getProgramArguments().addAll("--clean", "{clean}", "--create", "{dirty}", "--output", "{output}",
                "--patches", "{patches}", "--srg", "{srg}");
        
        getDistributionType().convention(DistributionType.JOINED);
        getOutputFileName().convention("output.lzma");
        
        getArguments().putRegularFile("clean", getClean());
        getArguments().putRegularFile("dirty", getPatched());
        getArguments().putRegularFile("srg", getMappings());
        
        getMultiArguments().putFiles("patches", getPatches());
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getClean();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPatched();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getPatches();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getMappings();

    @Input
    @Optional
    public abstract Property<DistributionType> getDistributionType();
}
