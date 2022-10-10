package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.mcp.naming.renamer.OfficialSourceRenamer;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;

@CacheableTask
public abstract class ApplyOfficialMappingsToSourceJar extends ApplyMappingsToSourceJarTask<OfficialSourceRenamer> implements IMcpRuntimeTask {

    public ApplyOfficialMappingsToSourceJar() {
        getOfficialNames().set(
                getClientMappings().flatMap(clientMappings ->
                        getServerMappings().flatMap(serverMappings ->
                                getTsrgMappings().map(TransformerUtils.guard(tsrgMappings ->
                                        OfficialSourceRenamer.from(clientMappings.getAsFile(), serverMappings.getAsFile(), tsrgMappings.getAsFile())))))
        );
        getRemapLambdas().convention(true);

        getOfficialNames().finalizeValueOnRead();
    }

    @Override
    protected OfficialSourceRenamer getRemappingArguments() {
        return getOfficialNames().get();
    }

    @Override
    protected byte[] createRemappedOutputOfSourceFile(final OfficialSourceRenamer sourceRenamer, byte[] inputStream, boolean shouldRemapJavadocs) throws IOException {
        return sourceRenamer.rename(inputStream, shouldRemapJavadocs, getRemapLambdas().getOrElse(true));
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

    @Internal
    public abstract Property<OfficialSourceRenamer> getOfficialNames();
}
