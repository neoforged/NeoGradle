package net.neoforged.gradle.mcp.naming.tasks;

import net.neoforged.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.mcp.naming.renamer.McpSourceRenamer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class ApplyMcpMappingsToSourceJar extends ApplyMappingsToSourceJar {

    public ApplyMcpMappingsToSourceJar() {
        getSourceRenamer().convention(getMappings().map(TransformerUtils.guard(m -> McpSourceRenamer.from(m.getAsFile()))));
        getRemapLambdas().convention(true);
        getSourceRenamer().finalizeValueOnRead();
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getMappings();
}
