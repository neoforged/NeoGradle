package net.neoforged.gradle.common.runtime.naming.tasks;

import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.naming.renamer.IMappingFileSourceRenamer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

@CacheableTask
public abstract class ApplyOfficialMappingsToSourceJar extends ApplyMappingsToSourceJar {

    public ApplyOfficialMappingsToSourceJar() {
        getSourceRenamer().convention(
                getClientMappingsFile().flatMap(clientMappings ->
                        getServerMappingsFile().map(TransformerUtils.guard(serverMappings ->
                                IMappingFileSourceRenamer.from(clientMappings.getAsFile(), serverMappings.getAsFile()))))
        );
        getRemapLambdas().convention(true);
        getSourceRenamer().finalizeValueOnRead();
    }

    @Input
    public abstract Property<Boolean> getRemapLambdas();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    @Optional
    public abstract RegularFileProperty getClientMappingsFile();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    @Optional
    public abstract RegularFileProperty getServerMappingsFile();
}
