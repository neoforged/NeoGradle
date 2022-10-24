package net.minecraftforge.gradle.mcp.naming.tasks;

import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.mcp.naming.renamer.McpSourceRenamer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;

@CacheableTask
public abstract class ApplyMcpMappingsToSourceJar extends ApplyMappingsToSourceJar<McpSourceRenamer> {

    public ApplyMcpMappingsToSourceJar() {
        getMcpNames().convention(getMappings().map(TransformerUtils.guard(m -> McpSourceRenamer.from(m.getAsFile()))));
        getRemapLambdas().convention(true);
    }

    @Override
    protected byte[] createRemappedOutputOfSourceFile(final McpSourceRenamer sourceRenamer, byte[] inputStream, boolean shouldRemapJavadocs) throws IOException {
        return sourceRenamer.rename(inputStream, shouldRemapJavadocs, getRemapLambdas().getOrElse(true));
    }

    @Input
    public abstract Property<Boolean> getRemapLambdas();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getMappings();

    @Internal
    public abstract Property<McpSourceRenamer> getMcpNames();
}
