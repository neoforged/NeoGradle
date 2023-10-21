package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested

@CompileStatic
interface RuntimeArguments extends ConfigurableDSLElement<RuntimeArguments> {

    @DSLProperty
    @Input
    abstract MapProperty<String, String> getSimple();

    @DSLProperty
    @Nested
    abstract ListProperty<NamedFileRef> getFiles();

    Provider<String> get(String key)

    Provider<String> getOrDefault(String key, Provider<String> defaultProvider)

    Provider<Map<String, Provider<String>>> asMap();

    void putFile(String input, Provider<File> fileProvider);

    void putDirectory(String input, Provider<File> fileProvider);

    void put(String input, Provider<String> stringProvider);
}