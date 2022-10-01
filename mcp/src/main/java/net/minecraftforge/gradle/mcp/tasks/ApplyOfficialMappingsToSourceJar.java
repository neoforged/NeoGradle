package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.tasks.ApplyMappingsToSourceJarTask;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.naming.renamer.OfficialSourceRenamer;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;

public abstract class ApplyOfficialMappingsToSourceJar extends ApplyMappingsToSourceJarTask {

    public ApplyOfficialMappingsToSourceJar() {
        getOfficialNames().convention(
                getClientMappings().flatMap(clientMappings ->
                        getServerMappings().flatMap(serverMappings ->
                                getTsrgMappings().map(TransformerUtils.guard(tsrgMappings ->
                                        OfficialSourceRenamer.from(clientMappings.getAsFile(), serverMappings.getAsFile(), tsrgMappings.getAsFile())))))
        );
        getTsrgMappings().fileProvider(getMcpRuntime().flatMap(McpRuntimeDefinition::getTsrgMappingsFile));
        getRemapLambdas().convention(true);
        getMcpRuntime().convention(
                getMcpRuntimeName().map(runtimeName -> {
                    final McpRuntimeExtension mcpMinecraftExtension = getProject().getExtensions().getByType(McpRuntimeExtension.class);
                    return mcpMinecraftExtension.getRuntimes().get().computeIfAbsent(runtimeName, name -> {
                        throw new IllegalStateException("Could not find the required mcp runtime: '%s' for task: %s".formatted(name, getName()));
                    });
                })
        );
        getMcpRuntimeName().convention("");
    }

    @Override
    protected byte[] createRemappedOutputOfSourceFile(byte[] inputStream, boolean shouldRemapJavadocs) throws IOException {
        return getOfficialNames().get().rename(inputStream, shouldRemapJavadocs, getRemapLambdas().getOrElse(true));
    }

    @Input
    public abstract Property<Boolean> getRemapLambdas();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getClientMappings();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getServerMappings();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getTsrgMappings();

    @Input
    public abstract Property<String> getMcpRuntimeName();

    @Nested
    public abstract Property<McpRuntimeDefinition> getMcpRuntime();

    @Nested
    public abstract Property<OfficialSourceRenamer> getOfficialNames();
}
