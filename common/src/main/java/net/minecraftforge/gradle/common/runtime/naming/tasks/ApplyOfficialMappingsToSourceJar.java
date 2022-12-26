package net.minecraftforge.gradle.common.runtime.naming.tasks;

import net.minecraftforge.gradle.common.runtime.naming.renamer.IMappingFileSourceRenamer;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

@CacheableTask
public abstract class ApplyOfficialMappingsToSourceJar extends ApplyMappingsToSourceJar implements Runtime {

    public ApplyOfficialMappingsToSourceJar() {
        getSourceRenamer().convention(
                getClientMappings().flatMap(clientMappings ->
                        getServerMappings().map(TransformerUtils.guard(serverMappings ->
                                IMappingFileSourceRenamer.from(clientMappings.getAsFile(), serverMappings.getAsFile()))))
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
}
