package net.minecraftforge.gradle.mcp.naming.tasks;

import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.minecraftforge.gradle.base.util.TransformerUtils;
import net.minecraftforge.gradle.mcp.naming.renamer.McpSourceRenamer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;

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
