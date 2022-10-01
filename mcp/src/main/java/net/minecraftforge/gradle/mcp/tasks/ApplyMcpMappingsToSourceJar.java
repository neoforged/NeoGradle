package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.tasks.ApplyMappingsToSourceJarTask;
import net.minecraftforge.gradle.mcp.naming.renamer.McpSourceRenamer;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;

public abstract class ApplyMcpMappingsToSourceJar extends ApplyMappingsToSourceJarTask {

    public ApplyMcpMappingsToSourceJar() {
        getMcpNames().convention(getMappings().map(TransformerUtils.guard(m -> McpSourceRenamer.from(m.getAsFile()))));
        getRemapLambdas().convention(true);
    }

    @Override
    protected byte[] createRemappedOutputOfSourceFile(byte[] inputStream, boolean shouldRemapJavadocs) throws IOException {
        return getMcpNames().get().rename(inputStream, shouldRemapJavadocs, getRemapLambdas().getOrElse(true));
    }

    @Input
    public abstract Property<Boolean> getRemapLambdas();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getMappings();

    @Nested
    public abstract Property<McpSourceRenamer> getMcpNames();
}
