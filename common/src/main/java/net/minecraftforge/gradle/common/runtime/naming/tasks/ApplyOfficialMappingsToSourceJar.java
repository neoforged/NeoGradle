package net.minecraftforge.gradle.common.runtime.naming.tasks;

import net.minecraftforge.gradle.common.runtime.naming.renamer.ISourceRenamer;
import net.minecraftforge.gradle.common.runtime.naming.renamer.IntermediaryMappingsFilteredOfficialSourceRenamer;
import net.minecraftforge.gradle.common.runtime.naming.renamer.UnfilteredOfficialSourceRenamer;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class ApplyOfficialMappingsToSourceJar extends ApplyMappingsToSourceJar implements IRuntimeTask {

    public ApplyOfficialMappingsToSourceJar() {
        getSourceRenamer().set(
                getClientMappings().flatMap(clientMappings ->
                        getServerMappings().flatMap(TransformerUtils.guard(serverMappings ->
                                getTsrgMappings().map(TransformerUtils.<ISourceRenamer, RegularFile>guard(tsrgMappings ->
                                                IntermediaryMappingsFilteredOfficialSourceRenamer.from(clientMappings.getAsFile(), serverMappings.getAsFile(), tsrgMappings.getAsFile())))
                                        .orElse(UnfilteredOfficialSourceRenamer.from(clientMappings.getAsFile(), serverMappings.getAsFile())))))
        );
        getRemapLambdas().convention(true);
        getSourceRenamer().finalizeValueOnRead();
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
    @Optional
    public abstract RegularFileProperty getTsrgMappings();

}
