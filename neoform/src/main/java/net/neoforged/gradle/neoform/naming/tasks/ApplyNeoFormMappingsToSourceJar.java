package net.neoforged.gradle.neoform.naming.tasks;

import net.neoforged.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.neoforged.gradle.neoform.naming.renamer.NeoFormSourceRenamer;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class ApplyNeoFormMappingsToSourceJar extends ApplyMappingsToSourceJar {

    public ApplyNeoFormMappingsToSourceJar() {
        getSourceRenamer().convention(getMappings().map(TransformerUtils.guard(m -> NeoFormSourceRenamer.from(m.getAsFile()))));
        getRemapLambdas().convention(true);
        getSourceRenamer().finalizeValueOnRead();
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getMappings();
}
