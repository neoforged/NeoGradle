package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CompileStatic
class NamedFile implements NamedFileRef {

    private final String name;
    private final Provider<File> file;

    NamedFile(String name, Provider<File> file) {
        this.name = name
        this.file = file
    }

    @Input
    String getName() {
        return name
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    Provider<File> getFile() {
        return file
    }
}
