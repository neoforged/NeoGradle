package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.util.Configurable

@CompileStatic
interface RuntimeMultiArguments extends ConfigurableDSLElement<RuntimeMultiArguments> {

    @DSLProperty
    @Input
    abstract MapProperty<String, List<String>> getSimple();

    @DSLProperty
    @Nested
    abstract ListProperty<NamedFiles> getFiles();

    Provider<List<String>> get(String key)

    Provider<List<String>> getOrDefault(String key, Provider<List<String>> defaultProvider)

    Provider<Map<String, Provider<List<String>>>> AsMap();

    void putFiles(String patches, ConfigurableFileCollection provider)

    void putSimple(String patches, Provider<List<String>> provider)
}