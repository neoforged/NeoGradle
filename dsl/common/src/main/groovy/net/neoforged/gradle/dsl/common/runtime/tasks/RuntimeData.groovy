package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested

@CompileStatic
interface RuntimeData extends ConfigurableDSLElement<RuntimeData> {

    @DSLProperty
    @Nested
    abstract ListProperty<NamedFileRef> getFiles();

    Provider<File> get(String key)

    Provider<File> getOrDefault(String key, Provider<File> defaultProvider)

    Provider<Map<String, Provider<File>>> asMap();

    void putFile(String input, Provider<File> fileProvider);

    void putDirectory(String input, Provider<File> fileProvider);

    void putAllFiles(Map<String, File> files);

    void putAllDirectories(Map<String, File> files);
}